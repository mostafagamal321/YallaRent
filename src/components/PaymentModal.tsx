import React, { useState } from 'react';
import { useApp } from '../context/AppContext';
import { Payment } from '../types';
import { toISODate, formatUIDate } from '../utils/date';
import { formatCurrency } from '../utils/number';
import { CheckCircle2, DollarSign, Calendar, FileText, X } from 'lucide-react';

interface PaymentModalProps {
  paymentId: string;
  onClose: () => void;
}

export const PaymentModal: React.FC<PaymentModalProps> = ({ paymentId, onClose }) => {
  const { payments, contracts, properties, units, language, markPaymentPaid, updatePayment } = useApp();
  const isAr = language === 'ar';

  const payment = payments.find((p) => p.id === paymentId);
  const contract = payment ? contracts.find((c) => c.id === payment.contractId) : undefined;
  const unit = payment ? units.find((u) => u.id === payment.unitId) : undefined;
  const property = unit ? properties.find((p) => p.id === unit.propertyId) : undefined;

  const [paidDate, setPaidDate] = useState<string>(payment?.paidDate || toISODate(new Date()));
  const [paidAmount, setPaidAmount] = useState<number>(payment?.paidAmount || payment?.amount || 0);
  const [note, setNote] = useState<string>(payment?.note || (isAr ? 'تم السداد نقداً بإيصال' : 'Paid in cash with receipt'));
  const [isPartial, setIsPartial] = useState(false);

  if (!payment) return null;

  const handleConfirmPaid = async () => {
    await markPaymentPaid(payment.id, Number(paidAmount), paidDate, note.trim() || undefined);
    onClose();
  };

  const handleMarkPending = async () => {
    await updatePayment({
      ...payment,
      status: 'pending',
      paidDate: null,
      paidAmount: undefined,
      note: undefined,
    });
    onClose();
  };

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        backgroundColor: 'rgba(0,0,0,0.6)',
        zIndex: 250,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '16px',
      }}
    >
      <div
        style={{
          backgroundColor: '#ffffff',
          borderRadius: '20px',
          padding: '24px',
          maxWidth: '420px',
          width: '100%',
          boxShadow: '0 10px 25px rgba(0,0,0,0.2)',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '14px' }}>
          <div>
            <h3 style={{ fontSize: '18px', fontWeight: 800, color: '#1e293b', margin: 0 }}>
              {isAr ? 'تسجيل سداد الإيجار' : 'Record Rent Payment'}
            </h3>
            <p style={{ fontSize: '12px', color: '#64748b', margin: '2px 0 0 0' }}>
              {property?.name} • {isAr ? `وحدة ${unit?.unitNumber}` : `Unit ${unit?.unitNumber}`}
            </p>
          </div>
          <button
            onClick={onClose}
            style={{
              width: '32px',
              height: '32px',
              borderRadius: '50%',
              backgroundColor: '#f1f5f9',
              border: 'none',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              cursor: 'pointer',
            }}
          >
            <X size={16} color="#64748b" />
          </button>
        </div>

        {/* Tenant Summary */}
        <div style={{ backgroundColor: '#f0fdf4', padding: '12px', borderRadius: '10px', marginBottom: '16px', border: '1px solid #bbf7d0' }}>
          <div style={{ fontSize: '15px', fontWeight: 800, color: '#166534', marginBottom: '4px' }}>
            {contract?.tenantName || (isAr ? 'مستأجر' : 'Tenant')}
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '12px', color: '#14532d' }}>
            <span>{isAr ? 'تاريخ الاستحقاق:' : 'Due date:'} <strong>{formatUIDate(payment.dueDate)}</strong></span>
            <span>{isAr ? 'المبلغ المطلوب:' : 'Amount:'} <strong>{formatCurrency(payment.amount, language)}</strong></span>
          </div>
        </div>

        {/* Form */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '14px', marginBottom: '20px' }}>
          <div>
            <label style={{ display: 'block', fontSize: '12px', fontWeight: 700, color: '#475569', marginBottom: '4px' }}>
              {isAr ? 'تاريخ السداد الفعلي' : 'Payment Date'}
            </label>
            <input
              type="date"
              value={paidDate}
              onChange={(e) => setPaidDate(e.target.value)}
              style={{
                width: '100%',
                height: '44px',
                padding: '0 12px',
                borderRadius: '8px',
                border: '1px solid #cbd5e1',
                fontSize: '14px',
                boxSizing: 'border-box',
              }}
            />
          </div>

          <div>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '4px' }}>
              <label style={{ fontSize: '12px', fontWeight: 700, color: '#475569' }}>
                {isAr ? 'المبلغ المسدد (ج.م)' : 'Paid Amount (EGP)'}
              </label>
              <button
                type="button"
                onClick={() => {
                  setIsPartial(!isPartial);
                  if (isPartial) setPaidAmount(payment.amount);
                }}
                style={{
                  background: 'none',
                  border: 'none',
                  color: '#0284c7',
                  fontSize: '11px',
                  fontWeight: 700,
                  cursor: 'pointer',
                }}
              >
                {isPartial ? (isAr ? 'سداد المبلغ بالكامل' : 'Pay Full Amount') : (isAr ? 'سداد جزئي' : 'Partial Payment')}
              </button>
            </div>
            <input
              type="number"
              value={paidAmount}
              onChange={(e) => setPaidAmount(Number(e.target.value))}
              style={{
                width: '100%',
                height: '44px',
                padding: '0 12px',
                borderRadius: '8px',
                border: '1px solid #cbd5e1',
                fontSize: '16px',
                fontWeight: 800,
                color: '#1b4332',
                boxSizing: 'border-box',
              }}
            />
            {paidAmount < payment.amount && (
              <p style={{ fontSize: '11px', color: '#c2410c', marginTop: '4px', fontWeight: 600 }}>
                {isAr
                  ? `متبقي من هذا القسط: ${formatCurrency(payment.amount - paidAmount, language)}`
                  : `Remaining balance: ${formatCurrency(payment.amount - paidAmount, language)}`}
              </p>
            )}
          </div>

          <div>
            <label style={{ display: 'block', fontSize: '12px', fontWeight: 700, color: '#475569', marginBottom: '4px' }}>
              {isAr ? 'ملاحظات وسند الاستلام' : 'Receipt / Notes'}
            </label>
            <input
              type="text"
              value={note}
              onChange={(e) => setNote(e.target.value)}
              placeholder={isAr ? 'مثال: تم السداد نقداً بإيصال رقم ١٢' : 'e.g. Receipt #12'}
              style={{
                width: '100%',
                height: '44px',
                padding: '0 12px',
                borderRadius: '8px',
                border: '1px solid #cbd5e1',
                fontSize: '13px',
                boxSizing: 'border-box',
              }}
            />
          </div>
        </div>

        {/* Action Buttons */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
          <button
            onClick={handleConfirmPaid}
            style={{
              width: '100%',
              padding: '13px',
              borderRadius: '12px',
              border: 'none',
              backgroundColor: '#1b4332',
              color: '#ffffff',
              fontSize: '14px',
              fontWeight: 800,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '6px',
              cursor: 'pointer',
            }}
          >
            <CheckCircle2 size={18} />
            <span>{isAr ? 'تأكيد وحفظ السداد' : 'Confirm Payment'}</span>
          </button>

          {payment.status === 'paid' && (
            <button
              onClick={handleMarkPending}
              style={{
                width: '100%',
                padding: '10px',
                borderRadius: '10px',
                border: '1px solid #fecaca',
                backgroundColor: '#fef2f2',
                color: '#dc2626',
                fontSize: '12px',
                fontWeight: 700,
                cursor: 'pointer',
              }}
            >
              {isAr ? 'إلغاء السداد وإعادته لمستحق' : 'Revert to Pending'}
            </button>
          )}
        </div>
      </div>
    </div>
  );
};
