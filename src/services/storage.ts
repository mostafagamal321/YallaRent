import { Preferences } from '@capacitor/preferences';
import { Share } from '@capacitor/share';
import { Property, Unit, Contract, Payment, AppSettings, Tenant } from '../types';
import { INITIAL_PROPERTIES, INITIAL_UNITS } from '../data/seedData';
import { DEFAULT_LANDLORD_DETAILS, DEFAULT_APARTMENT_TEMPLATE, DEFAULT_SHOPS_TEMPLATE } from '../data/defaultTemplates';

const KEYS = {
  PROPERTIES: 'rent_properties_v1',
  UNITS: 'rent_units_v1',
  TENANTS: 'rent_tenants_v1',
  CONTRACTS: 'rent_contracts_v1',
  PAYMENTS: 'rent_payments_v1',
  SETTINGS: 'rent_settings_v1',
  INITIALIZED: 'rent_is_initialized_v2', // bumped to v2 for empty rents & tenant migration
};

export const DEFAULT_SETTINGS: AppSettings = {
  language: 'ar',
  pinEnabled: false,
  pinCode: '1234',
  landlord: DEFAULT_LANDLORD_DETAILS,
  reminders: {
    rentDueDaysBefore: 3,
    rentDueOnDay: true,
    contractEndingDays1: 60,
    contractEndingDays2: 30,
    overdueDaily: true,
    reminderTime: '09:00',
  },
  templates: {
    apartments: DEFAULT_APARTMENT_TEMPLATE,
    shops: DEFAULT_SHOPS_TEMPLATE,
  },
};

// Safe storage wrapper
export async function getStorageItem<T>(key: string, defaultValue: T): Promise<T> {
  try {
    const { value } = await Preferences.get({ key });
    if (!value) return defaultValue;
    return JSON.parse(value) as T;
  } catch (e) {
    console.warn(`Error reading key ${key} from preferences:`, e);
    const local = localStorage.getItem(key);
    if (local) {
      try {
        return JSON.parse(local) as T;
      } catch {
        return defaultValue;
      }
    }
    return defaultValue;
  }
}

export async function setStorageItem<T>(key: string, value: T): Promise<void> {
  const jsonStr = JSON.stringify(value);
  try {
    await Preferences.set({ key, value: jsonStr });
  } catch (e) {
    console.warn(`Error saving key ${key} to preferences:`, e);
  }
  try {
    localStorage.setItem(key, jsonStr);
  } catch {
    // Ignore storage quota
  }
}

export interface AppDataBundle {
  version: number;
  exportedAt: string;
  properties: Property[];
  units: Unit[];
  tenants: Tenant[];
  contracts: Contract[];
  payments: Payment[];
  settings: AppSettings;
}

export async function loadInitialAppData(): Promise<AppDataBundle> {
  const { value: isInit } = await Preferences.get({ key: KEYS.INITIALIZED });
  
  if (!isInit) {
    // Seed initial properties & units, but start with empty rents as requested!
    const properties = INITIAL_PROPERTIES;
    const units = INITIAL_UNITS;
    const tenants: Tenant[] = [];
    const contracts: Contract[] = [];
    const payments: Payment[] = [];
    const settings = DEFAULT_SETTINGS;

    await setStorageItem(KEYS.PROPERTIES, properties);
    await setStorageItem(KEYS.UNITS, units);
    await setStorageItem(KEYS.TENANTS, tenants);
    await setStorageItem(KEYS.CONTRACTS, contracts);
    await setStorageItem(KEYS.PAYMENTS, payments);
    await setStorageItem(KEYS.SETTINGS, settings);
    await Preferences.set({ key: KEYS.INITIALIZED, value: 'true' });

    return {
      version: 2,
      exportedAt: new Date().toISOString(),
      properties,
      units,
      tenants,
      contracts,
      payments,
      settings,
    };
  }

  const [properties, units, loadedTenants, loadedContracts, payments, settings] = await Promise.all([
    getStorageItem<Property[]>(KEYS.PROPERTIES, INITIAL_PROPERTIES),
    getStorageItem<Unit[]>(KEYS.UNITS, INITIAL_UNITS),
    getStorageItem<Tenant[]>(KEYS.TENANTS, []),
    getStorageItem<Contract[]>(KEYS.CONTRACTS, []),
    getStorageItem<Payment[]>(KEYS.PAYMENTS, []),
    getStorageItem<AppSettings>(KEYS.SETTINGS, DEFAULT_SETTINGS),
  ]);

  // Migrate existing contracts to Tenant entities if needed
  let tenants = [...loadedTenants];
  const tenantMapByName = new Map<string, Tenant>();
  tenants.forEach((t) => tenantMapByName.set(t.fullName.trim(), t));

  let contractsUpdated = false;
  const migratedContracts = loadedContracts.map((c) => {
    if (c.tenantName && !c.tenantId) {
      let existing = tenantMapByName.get(c.tenantName.trim());
      if (!existing) {
        existing = {
          id: `tnt-${Date.now()}-${Math.floor(Math.random() * 1000)}`,
          fullName: c.tenantName.trim(),
          nationalId: c.nationalId || '',
          job: c.tenantJob || 'موظف',
          phone: '',
          address: c.tenantAddress || 'الفيوم',
          nationality: c.tenantNationality || 'مصري',
          notes: c.notes,
        };
        tenants.push(existing);
        tenantMapByName.set(existing.fullName.trim(), existing);
      }
      contractsUpdated = true;
      return { ...c, tenantId: existing.id };
    }
    return c;
  });

  if (contractsUpdated || tenants.length !== loadedTenants.length) {
    await setStorageItem(KEYS.TENANTS, tenants);
    await setStorageItem(KEYS.CONTRACTS, migratedContracts);
  }

  return {
    version: 2,
    exportedAt: new Date().toISOString(),
    properties,
    units,
    tenants,
    contracts: migratedContracts,
    payments,
    settings,
  };
}

export async function saveAppData(
  properties: Property[],
  units: Unit[],
  contracts: Contract[],
  payments: Payment[],
  settings: AppSettings,
  tenants: Tenant[] = []
): Promise<void> {
  await Promise.all([
    setStorageItem(KEYS.PROPERTIES, properties),
    setStorageItem(KEYS.UNITS, units),
    setStorageItem(KEYS.TENANTS, tenants),
    setStorageItem(KEYS.CONTRACTS, contracts),
    setStorageItem(KEYS.PAYMENTS, payments),
    setStorageItem(KEYS.SETTINGS, settings),
  ]);
}

// Export backup JSON file
export async function exportBackupFile(data: AppDataBundle): Promise<boolean> {
  const jsonContent = JSON.stringify(data, null, 2);
  const now = new Date();
  const dateTag = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
  const fileName = `RentManager-Backup-${dateTag}.json`;

  try {
    if (navigator.share) {
      const blob = new Blob([jsonContent], { type: 'application/json' });
      const file = new File([blob], fileName, { type: 'application/json' });
      if (navigator.canShare && navigator.canShare({ files: [file] })) {
        await navigator.share({
          title: 'Rent Manager Backup',
          text: `نسخة احتياطية لتطبيق إدارة الإيجارات - ${dateTag}`,
          files: [file],
        });
        return true;
      }
    }
  } catch (err) {
    console.log('Navigator share failed or unsupported, falling back to download:', err);
  }

  // Fallback download link
  try {
    const blob = new Blob([jsonContent], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = fileName;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
    return true;
  } catch (e) {
    console.error('Download fallback failed:', e);
    return false;
  }
}
