export type PropertyType = 'apartments' | 'shops';

export interface Property {
  id: string;
  name: string;
  type: PropertyType;
  address: string;
}

export interface Unit {
  id: string;
  propertyId: string;
  unitNumber: string;
  description: string;
  notes?: string; // previous/expected tenant or owner note
}

export type ContractStatus = 'active' | 'expiring_soon' | 'expired' | 'vacant';

export interface Contract {
  id: string;
  unitId: string;
  tenantName: string;
  nationalId: string; // 14 digits
  startDate: string; // YYYY-MM-DD
  endDate: string; // YYYY-MM-DD
  monthlyRent: number;
  rentDueDay: number; // 1 - 31
  notes?: string;
  status: ContractStatus;
  
  // Extra fields with defaults
  contractDate: string; // YYYY-MM-DD
  tenantJob: string;
  tenantAddress: string;
  tenantNationality: string;
  idIssuedFrom: string;
  usagePurpose: string;
  paymentTerms: string;
  renewalTerms: string;
  water: string;
  electricity: string;
  stairsLight: string;
  doormanFee: string;
  depositAmount: number;
  extraClause: string;
  
  // History tracking
  isHistory?: boolean;
  historyEndedDate?: string;
}

export type PaymentStatus = 'paid' | 'pending' | 'overdue';

export interface Payment {
  id: string;
  contractId: string;
  unitId: string;
  dueDate: string; // YYYY-MM-DD
  amount: number;
  paidAmount?: number;
  paidDate?: string | null; // YYYY-MM-DD
  status: PaymentStatus;
  note?: string;
}

export interface LandlordDetails {
  name: string;
  job: string;
  address: string;
  nationality: string;
}

export interface ReminderSettings {
  rentDueDaysBefore: number; // default 3
  rentDueOnDay: boolean; // default true
  contractEndingDays1: number; // default 60
  contractEndingDays2: number; // default 30
  overdueDaily: boolean; // default true
  reminderTime: string; // e.g. "09:00"
}

export interface ContractTemplates {
  apartments: string;
  shops: string;
}

export interface AppSettings {
  language: 'ar' | 'en';
  pinEnabled: boolean;
  pinCode: string;
  landlord: LandlordDetails;
  reminders: ReminderSettings;
  templates: ContractTemplates;
  lastBackupDate?: string;
}

export interface InAppNotification {
  id: string;
  type: 'rent_due' | 'overdue' | 'contract_expiring' | 'backup_reminder';
  title: string;
  message: string;
  date: string;
  relatedContractId?: string;
  relatedUnitId?: string;
  relatedPropertyId?: string;
  isRead?: boolean;
}
