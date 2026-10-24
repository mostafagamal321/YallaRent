import { toArabicIndicDigits } from './number';

// Parse any date string into Date object
export function parseDate(dateStr: string): Date {
  if (!dateStr) return new Date();
  
  // If format is dd/mm/yyyy
  if (dateStr.includes('/')) {
    const parts = dateStr.split('/');
    if (parts.length === 3) {
      if (parts[2].length === 4) {
        // dd/mm/yyyy
        return new Date(parseInt(parts[2], 10), parseInt(parts[1], 10) - 1, parseInt(parts[0], 10));
      } else if (parts[0].length === 4) {
        // yyyy/mm/dd
        return new Date(parseInt(parts[0], 10), parseInt(parts[1], 10) - 1, parseInt(parts[2], 10));
      }
    }
  }
  
  // Assume ISO YYYY-MM-DD
  const [year, month, day] = dateStr.split('-').map(Number);
  if (year && month) {
    return new Date(year, month - 1, day || 1);
  }
  return new Date(dateStr);
}

// Format as ISO YYYY-MM-DD
export function toISODate(date: Date): string {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

// Format for UI: dd/mm/yyyy
export function formatUIDate(dateStr: string): string {
  if (!dateStr) return '';
  const d = parseDate(dateStr);
  const day = String(d.getDate()).padStart(2, '0');
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const year = d.getFullYear();
  return `${day}/${month}/${year}`;
}

// Format for PDF: yyyy/mm/dd (with optional Arabic-Indic digits)
export function formatPDFDate(dateStr: string, useArabicIndic = true): string {
  if (!dateStr) return '';
  const d = parseDate(dateStr);
  const day = String(d.getDate()).padStart(2, '0');
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const year = String(d.getFullYear());
  const res = `${year}/${month}/${day}`;
  return useArabicIndic ? toArabicIndicDigits(res) : res;
}

// Calculate duration description
export function calculateDurationText(startDateStr: string, endDateStr: string, language: 'ar' | 'en' = 'ar'): string {
  const start = parseDate(startDateStr);
  const end = parseDate(endDateStr);
  
  // Add 1 day to end to account for inclusive end date (e.g. 01/09/2026 to 30/08/2027 is exactly 1 year)
  const adjustedEnd = new Date(end.getTime() + 24 * 60 * 60 * 1000);
  
  let months = (adjustedEnd.getFullYear() - start.getFullYear()) * 12 + (adjustedEnd.getMonth() - start.getMonth());
  const dayDiff = adjustedEnd.getDate() - start.getDate();
  if (dayDiff > 15) months += 1;
  
  if (language === 'ar') {
    if (months === 12) return 'سنة';
    if (months === 24) return 'سنتين';
    if (months === 36) return 'ثلاث سنوات';
    if (months === 48) return 'أربع سنوات';
    if (months === 60) return 'خمس سنوات';
    if (months % 12 === 0) return `${months / 12} سنوات`;
    if (months === 1) return 'شهر واحد';
    if (months === 2) return 'شهرين';
    if (months >= 3 && months <= 10) return `${months} أشهر`;
    return `${months} شهراً`;
  } else {
    if (months === 12) return '1 Year';
    if (months === 24) return '2 Years';
    if (months % 12 === 0) return `${months / 12} Years`;
    return `${months} Months`;
  }
}

// Generate monthly due dates between startDate and endDate using rentDueDay
export function generateMonthlyPaymentDates(startDateStr: string, endDateStr: string, dueDay: number): string[] {
  const dates: string[] = [];
  const start = parseDate(startDateStr);
  const end = parseDate(endDateStr);
  
  const current = new Date(start.getFullYear(), start.getMonth(), 1);
  const finalMonth = new Date(end.getFullYear(), end.getMonth(), 1);
  
  while (current <= finalMonth) {
    const year = current.getFullYear();
    const month = current.getMonth();
    
    // Find the last day of this month
    const lastDayOfMonth = new Date(year, month + 1, 0).getDate();
    const actualDueDay = Math.min(dueDay, lastDayOfMonth);
    const dueDate = new Date(year, month, actualDueDay);
    
    dates.push(toISODate(dueDate));
    current.setMonth(current.getMonth() + 1);
  }
  
  return dates;
}

// Check difference in days from today (positive = in future, negative = past)
export function getDaysFromToday(dateStr: string): number {
  const target = parseDate(dateStr);
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  target.setHours(0, 0, 0, 0);
  
  const diffTime = target.getTime() - today.getTime();
  return Math.round(diffTime / (1000 * 60 * 60 * 24));
}
