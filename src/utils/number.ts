export function toArabicIndicDigits(num: string | number): string {
  const str = String(num);
  const arabicDigits = ['٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩'];
  return str.replace(/[0-9]/g, (w) => arabicDigits[+w]);
}

export function formatNumber(value: number | string, useArabicIndic = false): string {
  const num = typeof value === 'string' ? parseFloat(value) || 0 : value;
  const formatted = new Intl.NumberFormat('en-US').format(num);
  return useArabicIndic ? toArabicIndicDigits(formatted) : formatted;
}

export function formatCurrency(amount: number, language: 'ar' | 'en' = 'ar'): string {
  const formatted = new Intl.NumberFormat(language === 'ar' ? 'ar-EG' : 'en-US', {
    useGrouping: true,
  }).format(amount);
  
  if (language === 'ar') {
    return `${formatted} ج.م`;
  }
  return `${formatted} EGP`;
}
