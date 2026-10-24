import { LocalNotifications } from '@capacitor/local-notifications';
import { Contract, Payment, Property, Unit, InAppNotification, ReminderSettings } from '../types';
import { getDaysFromToday, formatUIDate } from '../utils/date';
import { formatCurrency } from '../utils/number';

// Request notification permission safely
export async function requestNotificationPermissions(): Promise<boolean> {
  try {
    const status = await LocalNotifications.checkPermissions();
    if (status.display === 'granted') return true;
    const request = await LocalNotifications.requestPermissions();
    return request.display === 'granted';
  } catch (e) {
    console.log('LocalNotifications permission check failed/skipped:', e);
    return false;
  }
}

// Generate in-app notifications and schedule reminders
export function computeInAppNotifications(
  contracts: Contract[],
  payments: Payment[],
  properties: Property[],
  units: Unit[],
  reminders: ReminderSettings,
  lastBackupDate?: string,
  language: 'ar' | 'en' = 'ar'
): InAppNotification[] {
  const notifications: InAppNotification[] = [];
  const propertyMap = new Map(properties.map((p) => [p.id, p]));
  const unitMap = new Map(units.map((u) => [u.id, u]));

  // 1. Overdue payments
  const overduePayments = payments.filter((p) => p.status === 'overdue');
  for (const pay of overduePayments) {
    const unit = unitMap.get(pay.unitId);
    const prop = unit ? propertyMap.get(unit.propertyId) : undefined;
    const contract = contracts.find((c) => c.id === pay.contractId);

    const title = language === 'ar' ? 'إيجار متأخر يستوجب التحصيل' : 'Overdue Rent Payment';
    const message =
      language === 'ar'
        ? `المستأجر: ${contract?.tenantName || 'غير معروف'} - ${prop?.name || ''} (وحدة ${unit?.unitNumber || ''}) بمبلغ ${formatCurrency(pay.amount, 'ar')} مستحق منذ ${formatUIDate(pay.dueDate)}`
        : `Tenant: ${contract?.tenantName || 'Unknown'} - ${prop?.name || ''} (Unit ${unit?.unitNumber || ''}) amount ${formatCurrency(pay.amount, 'en')} due since ${formatUIDate(pay.dueDate)}`;

    notifications.push({
      id: `overdue-${pay.id}`,
      type: 'overdue',
      title,
      message,
      date: pay.dueDate,
      relatedContractId: contract?.id,
      relatedUnitId: unit?.id,
      relatedPropertyId: prop?.id,
    });
  }

  // 2. Rent due in upcoming days (e.g. within 3 days or today)
  const pendingPayments = payments.filter((p) => p.status === 'pending');
  for (const pay of pendingPayments) {
    const daysLeft = getDaysFromToday(pay.dueDate);
    if (daysLeft >= 0 && daysLeft <= reminders.rentDueDaysBefore) {
      const unit = unitMap.get(pay.unitId);
      const prop = unit ? propertyMap.get(unit.propertyId) : undefined;
      const contract = contracts.find((c) => c.id === pay.contractId);

      const title =
        daysLeft === 0
          ? language === 'ar'
            ? 'استحقاق إيجار اليوم'
            : 'Rent Due Today'
          : language === 'ar'
          ? `استحقاق إيجار خلال ${daysLeft} أيام`
          : `Rent Due in ${daysLeft} days`;

      const message =
        language === 'ar'
          ? `المستأجر: ${contract?.tenantName || 'غير معروف'} - ${prop?.name || ''} (وحدة ${unit?.unitNumber || ''}) بمبلغ ${formatCurrency(pay.amount, 'ar')}`
          : `Tenant: ${contract?.tenantName || 'Unknown'} - ${prop?.name || ''} (Unit ${unit?.unitNumber || ''}) amount ${formatCurrency(pay.amount, 'en')}`;

      notifications.push({
        id: `due-${pay.id}`,
        type: 'rent_due',
        title,
        message,
        date: pay.dueDate,
        relatedContractId: contract?.id,
        relatedUnitId: unit?.id,
        relatedPropertyId: prop?.id,
      });
    }
  }

  // 3. Contracts expiring soon (within 60 days)
  for (const contract of contracts) {
    if (contract.status === 'active' || contract.status === 'expiring_soon') {
      const daysLeft = getDaysFromToday(contract.endDate);
      if (daysLeft >= 0 && daysLeft <= reminders.contractEndingDays1) {
        const unit = unitMap.get(contract.unitId);
        const prop = unit ? propertyMap.get(unit.propertyId) : undefined;

        const title =
          language === 'ar'
            ? `عقد إيجار ينتهي قريباً (${daysLeft} يوم)`
            : `Contract Expiring Soon (${daysLeft} days)`;

        const message =
          language === 'ar'
            ? `ينتهي عقد المستأجر: ${contract.tenantName} - ${prop?.name || ''} (وحدة ${unit?.unitNumber || ''}) في تاريخ ${formatUIDate(contract.endDate)}`
            : `Contract for ${contract.tenantName} - ${prop?.name || ''} (Unit ${unit?.unitNumber || ''}) ends on ${formatUIDate(contract.endDate)}`;

        notifications.push({
          id: `contract-exp-${contract.id}`,
          type: 'contract_expiring',
          title,
          message,
          date: contract.endDate,
          relatedContractId: contract.id,
          relatedUnitId: unit?.id,
          relatedPropertyId: prop?.id,
        });
      }
    }
  }

  // 4. Monthly backup reminder
  if (!lastBackupDate) {
    notifications.push({
      id: 'backup-reminder',
      type: 'backup_reminder',
      title: language === 'ar' ? 'تذكير بأخذ نسخة احتياطية' : 'Backup Reminder',
      message:
        language === 'ar'
          ? 'يُنصح بحفظ نسخة احتياطية من بيانات العقود والإيجارات شهرياً من شاشة الإعدادات.'
          : 'It is recommended to export a monthly backup of your rental data from Settings.',
      date: new Date().toISOString(),
    });
  } else {
    const daysSinceBackup = Math.abs(getDaysFromToday(lastBackupDate));
    if (daysSinceBackup >= 30) {
      notifications.push({
        id: 'backup-reminder-monthly',
        type: 'backup_reminder',
        title: language === 'ar' ? 'تذكير بالنسخ الاحتياطي الشهري' : 'Monthly Backup Reminder',
        message:
          language === 'ar'
            ? `مر ${daysSinceBackup} يوماً منذ آخر نسخة احتياطية. احرص على حفظ بياناتك الآن.`
            : `It has been ${daysSinceBackup} days since your last backup. Keep your data safe.`,
        date: new Date().toISOString(),
      });
    }
  }

  return notifications;
}

// Reschedule capacitor local notifications
export async function scheduleDeviceNotifications(
  notifications: InAppNotification[]
): Promise<void> {
  try {
    const isAvailable = await requestNotificationPermissions();
    if (!isAvailable) return;

    // Cancel pending notifications first
    const pending = await LocalNotifications.getPending();
    if (pending.notifications.length > 0) {
      await LocalNotifications.cancel({ notifications: pending.notifications });
    }

    const itemsToSchedule = notifications.slice(0, 10).map((n, idx) => ({
      id: idx + 100,
      title: n.title,
      body: n.message,
      schedule: { at: new Date(Date.now() + (idx + 1) * 60 * 1000) },
      sound: undefined,
    }));

    if (itemsToSchedule.length > 0) {
      await LocalNotifications.schedule({ notifications: itemsToSchedule });
    }
  } catch (e) {
    console.log('Device notification scheduling skipped:', e);
  }
}
