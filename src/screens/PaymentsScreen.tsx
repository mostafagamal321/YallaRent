import React, { useState, useMemo } from 'react';
import { useApp } from '../context/AppContext';
import { Payment } from '../types';
import { formatCurrency } from '../utils/number';
import { formatUIDate, getDaysFromToday, toISODate } from '../utils/date';
import {
  ReceiptText,
  CheckCircle2,
  AlertTriangle,
  Clock,
  Filter,
  Check,
  Building,
  Calendar,
} from 'lucide-react';

interface PaymentsScreenProps {
  onOpenPaymentModal: (paymentId: string) => void;
}

export const PaymentsScreen: React.FC<PaymentsScreenProps> = ({ onOpenPaymentModal }) => {
  const { payments, contracts, properties, units, language, markPaymentPaid } = useApp();
  const isAr = language === 'ar';

  const [statusFilter, setStatusFilter] = useState<'all' | 'overdue' | 'pending' | 'paid'>('all');
  const [propertyFilter, setPropertyFilter] = useState<string>('all');
  const [monthFilter, setMonthFilter] = useState<string>('all');

  const propertyMap = new Map(properties.map((p) => [p.id, p]));
  const unitMap = new Map(units.map((u) => [u.id, u]));
  const contractMap = new Map(contracts.map((c) => [c.id, c]));

  // Available months from payments
  const availableMonths = useMemo(() => {
    const set = new Set<string>();
    for (const p of payments) {
      set.add(p.dueDate.slice(0, 7)); // YYYY-MM
    }
    return Array.from(set).sort().reverse();
  }, [payments]);

  // Filtered payments
  const filteredPayments = useMemo(() => {
    return payments
      .filter((p) => {
        if (statusFilter !== 'all' && p.status !== statusFilter) return false;
        if (monthFilter !== 'all' && !p.dueDate.startsWith(monthFilter)) return false;
        if (propertyFilter !== 'all') {
          const u = unitMap.get(p.unitId);
          if (u?.propertyId !== propertyFilter) return false;
        }
        return true;
      })
      .sort((a, b) => {
        // Overdue first, then by dueDate descending
        if (a.status === 'overdue' && b.status !== 'overdue') return -1;
        if (b.status === 'overdue' && a.status !== 'overdue') return 1;
        return a.dueDate.localeCompare(b.dueDate);
      });
  }, [payments, statusFilter, monthFilter, propertyFilter, unitMap]);

  // Totals for filtered list
  const totalAmount = filteredPayments.reduce((sum, p) => sum + p.amount, 0);
  const totalCollected = filteredPayments
    .filter((p) => p.status === 'paid')
    .reduce((sum, p) => sum + (p.paidAmount || p.amount), 0);
  const totalOverdue = filteredPayments
    .filter((p) => p.status === 'overdue')
    .reduce((sum, p) => sum + (p.amount - (p.paidAmount || 0)), 0);

  const handleQuickPay = async (e: React.MouseEvent, p: Payment) => {
    e.stopPropagation();
    await markPaymentPaid(p.id, p.amount, toISODate(new Date()), isAr ? 'تم السداد نقداً' : 'Paid in cash');
  };

  return (
    <div style={{ padding: '16px', paddingBottom: '90px' }}>
      {/* Title */}
      <div style={{ marginBottom: '14px' }}>
        <h2 style={{ fontSize: '20px', fontWeight: 800, color: '#1e293b', margin: 0 }}>
          {isAr ? 'سجل تحصيل الإيجارات' : 'Rent Collection Ledger'}
        </h2>
        <p style={{ fontSize: '13px', color: '#64748b', margin: '2px 0 0 0' }}>
          {isAr ? 'إدارة وتوثيق سداد الأقساط الشهرية والمتأخرات' : 'Track and collect monthly rent installments'}
        </p>
      </div>

      {/* Summary Card */}
      <div
        style={{
          backgroundColor: '#1b4332',
          borderRadius: '16px',
          padding: '16px',
          color: '#ffffff',
          marginBottom: '16px',
          boxShadow: '0 4px 12px rgba(27,67,50,0.18)',
        }}
      >
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '8px', textAlign: 'center' }}>
          <div>
            <div style={{ fontSize: '11px', color: '#a7f3d0' }}>{isAr ? 'إجمالي المطلوب' : 'Total Due'}</div>
            <div style={{ fontSize: '16px', fontWeight: 800, marginTop: '2px' }}>
              {formatCurrency(totalAmount, language)}
            </div>
          </div>
          <div style={{ borderLeft: '1px solid rgba(255,255,255,0.2)', borderRight: '1px solid rgba(255,255,255,0.2)' }}>
            <div style={{ fontSize: '11px', color: '#86efac' }}>{isAr ? 'المحصل' : 'Collected'}</div>
            <div style={{ fontSize: '16px', fontWeight: 800, color: '#4ade80', marginTop: '2px' }}>
              {formatCurrency(totalCollected, language)}
            </div>
          </div>
          <div>
            <div style={{ fontSize: '11px', color: '#fca5a5' }}>{isAr ? 'المتأخرات' : 'Overdue'}</div>
            <div style={{ fontSize: '16px', fontWeight: 800, color: '#f87171', marginTop: '2px' }}>
              {formatCurrency(totalOverdue, language)}
            </div>
          </div>
        </div>
      </div>

      {/* Filter Row 1: Status Chips */}
      <div style={{ display: 'flex', gap: '8px', overflowX: 'auto', paddingBottom: '8px', marginBottom: '10px' }}>
        {[
          { id: 'all', labelAr: 'الكل', labelEn: 'All' },
          { id: 'overdue', labelAr: 'متأخرات', labelEn: 'Overdue' },
          { id: 'pending', labelAr: 'مستحق', labelEn: 'Pending' },
          { id: 'paid', labelAr: 'تم سداده', labelEn: 'Paid' },
        ].map((chip) => (
          <button
            key={chip.id}
            onClick={() => setStatusFilter(chip.id as any)}
            style={{
              padding: '6px 14px',
              borderRadius: '20px',
              border: statusFilter === chip.id ? '1px solid #1b4332' : '1px solid #cbd5e1',
              backgroundColor: statusFilter === chip.id ? '#1b4332' : '#ffffff',
              color: statusFilter === chip.id ? '#ffffff' : '#475569',
              fontSize: '12px',
              fontWeight: 700,
              cursor: 'pointer',
              whiteSpace: 'nowrap',
            }}
          >
            {isAr ? chip.labelAr : chip.labelEn}
          </button>
        ))}
      </div>

      {/* Filter Row 2: Property & Month Selectors */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px', marginBottom: '16px' }}>
        <select
          value={propertyFilter}
          onChange={(e) => setPropertyFilter(e.target.value)}
          style={{
            height: '40px',
            padding: '0 8px',
            borderRadius: '8px',
            border: '1px solid #cbd5e1',
            fontSize: '12px',
            backgroundColor: '#ffffff',
            fontWeight: 600,
          }}
        >
          <option value="all">{isAr ? 'جميع العقارات' : 'All Properties'}</option>
          {properties.map((p) => (
            <option key={p.id} value={p.id}>{p.name}</option>
          ))}
        </select>

        <select
          value={monthFilter}
          onChange={(e) => setMonthFilter(e.target.value)}
          style={{
            height: '40px',
            padding: '0 8px',
            borderRadius: '8px',
            border: '1px solid #cbd5e1',
            fontSize: '12px',
            backgroundColor: '#ffffff',
            fontWeight: 600,
          }}
        >
          <option value="all">{isAr ? 'جميع الشهور' : 'All Months'}</option>
          {availableMonths.map((m) => (
            <option key={m} value={m}>{m}</option>
          ))}
        </select>
      </div>

      {/* Payments List */}
      {filteredPayments.length === 0 ? (
        <div
          style={{
            backgroundColor: '#ffffff',
            borderRadius: '16px',
            padding: '32px 16px',
            textAlign: 'center',
            color: '#64748b',
            border: '1px solid #e2e8f0',
          }}
        >
          {isAr ? 'لا توجد دفعات مسجلة تطابق هذه التصفية.' : 'No payments found matching this filter.'}
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
          {filteredPayments.map((p) => {
            const contract = contractMap.get(p.contractId);
            const unit = unitMap.get(p.unitId);
            const prop = unit ? propertyMap.get(unit.propertyId) : undefined;

            const isPaid = p.status === 'paid';
            const isOverdue = p.status === 'overdue';

            return (
              <div
                key={p.id}
                onClick={() => onOpenPaymentModal(p.id)}
                style={{
                  backgroundColor: '#ffffff',
                  borderRadius: '14px',
                  padding: '14px',
                  border: isOverdue ? '1.5px solid #fca5a5' : '1px solid #e2e8f0',
                  boxShadow: '0 2px 6px rgba(0,0,0,0.03)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  cursor: 'pointer',
                }}
              >
                <div style={{ flex: 1 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px' }}>
                    <span
                      style={{
                        fontSize: '11px',
                        fontWeight: 800,
                        padding: '2px 8px',
                        borderRadius: '6px',
                        backgroundColor: isPaid ? '#dcfce7' : isOverdue ? '#fee2e2' : '#fef3c7',
                        color: isPaid ? '#15803d' : isOverdue ? '#b91c1c' : '#b45309',
                      }}
                    >
                      {isPaid ? (isAr ? 'مسدد' : 'Paid') : isOverdue ? (isAr ? 'متأخر' : 'Overdue') : (isAr ? 'مستحق' : 'Pending')}
                    </span>
                    <span style={{ fontSize: '15px', fontWeight: 800, color: '#0f172a' }}>
                      {contract?.tenantName || (isAr ? 'مستأجر' : 'Tenant')}
                    </span>
                  </div>

                  <div style={{ fontSize: '12px', color: '#64748b' }}>
                    {prop?.name} • {isAr ? `وحدة ${unit?.unitNumber}` : `Unit ${unit?.unitNumber}`} • {isAr ? 'الاستحقاق:' : 'Due:'} <strong>{formatUIDate(p.dueDate)}</strong>
                  </div>

                  {isPaid && p.paidDate && (
                    <div style={{ fontSize: '11px', color: '#16a34a', marginTop: '2px' }}>
                      {isAr ? `سُدد في ${formatUIDate(p.paidDate)}` : `Paid on ${formatUIDate(p.paidDate)}`} {p.note ? `• ${p.note}` : ''}
                    </div>
                  )}
                </div>

                <div style={{ textAlign: isAr ? 'left' : 'right', marginLeft: '12px', marginRight: '12px' }}>
                  <div style={{ fontSize: '16px', fontWeight: 800, color: isOverdue ? '#dc2626' : '#1b4332' }}>
                    {formatCurrency(p.amount, language)}
                  </div>

                  {!isPaid ? (
                    <button
                      onClick={(e) => handleQuickPay(e, p)}
                      style={{
                        backgroundColor: '#1b4332',
                        color: '#ffffff',
                        border: 'none',
                        borderRadius: '8px',
                        padding: '6px 12px',
                        fontSize: '11px',
                        fontWeight: 700,
                        marginTop: '4px',
                        cursor: 'pointer',
                        display: 'flex',
                        alignItems: 'center',
                        gap: '4px',
                      }}
                    >
                      <Check size={12} />
                      <span>{isAr ? 'سداد سريع' : 'Quick Pay'}</span>
                    </button>
                  ) : (
                    <span style={{ fontSize: '11px', color: '#16a34a', fontWeight: 700 }}>
                      ✓ {isAr ? 'مكتمل' : 'Completed'}
                    </span>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};
