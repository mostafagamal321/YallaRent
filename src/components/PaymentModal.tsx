import React, { useState } from 'react';
import { useApp } from '../context/AppContext';
import { toISODate, formatUIDate } from '../utils/date';
import { formatCurrency } from '../utils/number';
import { CheckCircle2, Calendar, FileText, X, Printer, Trash2 } from 'lucide-react';
import { generateAndShareReceiptPDF } from '../services/pdfGenerator';

interface PaymentModalProps {
  paymentId: string;
  onClose: () => void;
}

export const PaymentModal: React.FC<PaymentModalProps> = ({ paymentId, onClose }) => {
  const {
    payments,
    contracts,
    properties,
    units,
    tenants,
    settings,
    language,
    markPaymentPaid,
    updatePayment,
    deletePayment,
  } = useApp();
  const isAr = language === 'ar';

  const payment = payments.find((p) => p.id === paymentId);
  const contract = payment ? contracts.find((c) => c.id === payment.contractId) : undefined;
  const unit = payment ? units.find((u) => u.id === payment.unitId) : undefined;
  const property = unit ? properties.find((p) => p.id === unit.propertyId) : undefined;
  const tenant = contract?.tenantId
    ? tenants.find((t) => t.id === contract.tenantId)
    : undefined;

  const tenantDisplayName =
    tenant?.fullName || contract?.tenantName || (isAr ? 'المستأجر' : 'Tenant');

  // Itemized amounts
  const [rentAmount, setRentAmount] = useState<number>(
    payment?.rentAmount !== undefined ? payment.rentAmount : (payment?.amount || contract?.monthlyRent || 0)
  );
  const [electricityAmount, setElectricityAmount] = useState<number>(
    payment?.electricityAmount || 0
  );
  const [waterAmount, setWaterAmount] = useState<number>(
    payment?.waterAmount || 0
  );
  const [stairsCleaningAmount, setStairsCleaningAmount] = useState<number>(
    payment?.stairsCleaningAmount || 0
  );
  const [previousArrearsAmount, setPreviousArrearsAmount] = useState<number>(
    payment?.previousArrearsAmount || 0
  );

  const [paidDate, setPaidDate] = useState<string>(
    payment?.paidDate || toISODate(new Date())
  );
  const [note, setNote] = useState<string>(
    payment?.note || (isAr ? 'تم التحصيل والسداد نقداً' : 'Collected in cash')
  );
  const [isGeneratingPDF, setIsGeneratingPDF] = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);

  if (!payment) return null;

  const totalCalculated =
    Number(rentAmount || 0) +
    Number(electricityAmount || 0) +
    Number(waterAmount || 0) +
    Number(stairsCleaningAmount || 0) +
    Number(previousArrearsAmount || 0);

  const handleConfirmPaid = async () => {
    await markPaymentPaid(payment.id, totalCalculated, paidDate, note.trim() || undefined, {
      rentAmount: Number(rentAmount || 0),
      electricityAmount: Number(electricityAmount || 0),
      waterAmount: Number(waterAmount || 0),
      stairsCleaningAmount: Number(stairsCleaningAmount || 0),
      previousArrearsAmount: Number(previousArrearsAmount || 0),
    });
    onClose();
  };

  const handlePrintReceipt = async () => {
    setIsGeneratingPDF(true);
    try {
      // First save if not yet saved with these values
      await markPaymentPaid(payment.id, totalCalculated, paidDate, note.trim() || undefined, {
        rentAmount: Number(rentAmount || 0),
        electricityAmount: Number(electricityAmount || 0),
        waterAmount: Number(waterAmount || 0),
        stairsCleaningAmount: Number(stairsCleaningAmount || 0),
        previousArrearsAmount: Number(previousArrearsAmount || 0),
      });

      await generateAndShareReceiptPDF({
        payment: {
          ...payment,
          amount: totalCalculated,
          paidAmount: totalCalculated,
          paidDate,
          monthYear: payment.monthYear || formatUIDate(payment.dueDate),
        },
        tenantName: tenantDisplayName,
        tenantPhone: tenant?.phone || '',
        propertyName: property?.name || '',
        unitNumber: unit?.unitNumber || '',
        landlord: settings.landlord,
        breakdown: {
          rentAmount: Number(rentAmount || 0),
          electricityAmount: Number(electricityAmount || 0),
          waterAmount: Number(waterAmount || 0),
          stairsCleaningAmount: Number(stairsCleaningAmount || 0),
          previousArrearsAmount: Number(previousArrearsAmount || 0),
          totalAmount: totalCalculated,
        },
        currentDate: paidDate,
        notes: note,
      });
    } catch (err) {
      console.error('Print receipt failed:', err);
    } finally {
      setIsGeneratingPDF(false);
    }
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

  const handleDelete = async () => {
    await deletePayment(payment.id);
    onClose();
  };

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        backgroundColor: 'rgba(0,0,0,0.65)',
        zIndex: 250,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '16px',
        overflowY: 'auto',
      }}
    >
      <div
        style={{
          backgroundColor: '#ffffff',
          borderRadius: '20px',
          padding: '24px',
          maxWidth: '460px',
          width: '100%',
          maxHeight: '92vh',
          overflowY: 'auto',
          boxShadow: '0 12px 30px rgba(0,0,0,0.25)',
        }}
      >
        {/* Header */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            marginBottom: '14px',
          }}
        >
          <div>
            <h3 style={{ fontSize: '18px', fontWeight: 800, color: '#1e293b', margin: 0 }}>
              {isAr ? 'التحصيل وتفاصيل الدفع' : 'Payment Collection'}
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

        {/* Tenant Summary Banner */}
        <div
          style={{
            backgroundColor: '#f0fdf4',
            padding: '12px 14px',
            borderRadius: '12px',
            marginBottom: '16px',
            border: '1px solid #bbf7d0',
          }}
        >
          <div style={{ fontSize: '16px', fontWeight: 800, color: '#166534', marginBottom: '4px' }}>
            {tenantDisplayName}
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '12px', color: '#14532d' }}>
            <span>
              {isAr ? 'تاريخ الاستحقاق:' : 'Due date:'} <strong>{formatUIDate(payment.dueDate)}</strong>
            </span>
            <span>
              {isAr ? 'الإيجار الأساسي:' : 'Base rent:'}{' '}
              <strong>{formatCurrency(payment.amount, language)}</strong>
            </span>
          </div>
        </div>

        {/* Itemized breakdown inputs */}
        <div style={{ marginBottom: '16px' }}>
          <div
            style={{
              fontSize: '13px',
              fontWeight: 800,
              color: '#1b4332',
              marginBottom: '8px',
              borderBottom: '1px solid #e2e8f0',
              paddingBottom: '4px',
            }}
          >
            {isAr ? 'تقسيم بنود التحصيل:' : 'Itemized Collection Breakdown:'}
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
            {/* Rent */}
            <div>
              <label style={{ display: 'block', fontSize: '11px', fontWeight: 700, color: '#475569', marginBottom: '3px' }}>
                {isAr ? '١. الإيجار الشهري (ج.م)' : '1. Rent (EGP)'}
              </label>
              <input
                type="number"
                value={rentAmount}
                onChange={(e) => setRentAmount(Number(e.target.value))}
                style={{
                  width: '100%',
                  height: '40px',
                  padding: '0 10px',
                  borderRadius: '8px',
                  border: '1.5px solid #cbd5e1',
                  fontSize: '14px',
                  fontWeight: 700,
                  boxSizing: 'border-box',
                }}
              />
            </div>

            {/* Electricity */}
            <div>
              <label style={{ display: 'block', fontSize: '11px', fontWeight: 700, color: '#475569', marginBottom: '3px' }}>
                {isAr ? '٢. استهلاك الكهرباء (ج.م)' : '2. Electricity (EGP)'}
              </label>
              <input
                type="number"
                value={electricityAmount}
                onChange={(e) => setElectricityAmount(Number(e.target.value))}
                placeholder="0"
                style={{
                  width: '100%',
                  height: '40px',
                  padding: '0 10px',
                  borderRadius: '8px',
                  border: '1.5px solid #cbd5e1',
                  fontSize: '14px',
                  fontWeight: 700,
                  boxSizing: 'border-box',
                }}
              />
            </div>

            {/* Water */}
            <div>
              <label style={{ display: 'block', fontSize: '11px', fontWeight: 700, color: '#475569', marginBottom: '3px' }}>
                {isAr ? '٣. استهلاك المياه (ج.م)' : '3. Water (EGP)'}
              </label>
              <input
                type="number"
                value={waterAmount}
                onChange={(e) => setWaterAmount(Number(e.target.value))}
                placeholder="0"
                style={{
                  width: '100%',
                  height: '40px',
                  padding: '0 10px',
                  borderRadius: '8px',
                  border: '1.5px solid #cbd5e1',
                  fontSize: '14px',
                  fontWeight: 700,
                  boxSizing: 'border-box',
                }}
              />
            </div>

            {/* Stairs Cleaning */}
            <div>
              <label style={{ display: 'block', fontSize: '11px', fontWeight: 700, color: '#475569', marginBottom: '3px' }}>
                {isAr ? '٤. نظافة وخدمات السلم (ج.م)' : '4. Stairs Cleaning (EGP)'}
              </label>
              <input
                type="number"
                value={stairsCleaningAmount}
                onChange={(e) => setStairsCleaningAmount(Number(e.target.value))}
                placeholder="0"
                style={{
                  width: '100%',
                  height: '40px',
                  padding: '0 10px',
                  borderRadius: '8px',
                  border: '1.5px solid #cbd5e1',
                  fontSize: '14px',
                  fontWeight: 700,
                  boxSizing: 'border-box',
                }}
              />
            </div>
          </div>

          {/* Arrears */}
          <div style={{ marginTop: '10px' }}>
            <label style={{ display: 'block', fontSize: '11px', fontWeight: 700, color: '#475569', marginBottom: '3px' }}>
              {isAr ? '٥. مبالغ متأخرة سابقة (ج.م)' : '5. Previously Unpaid Amount / Arrears (EGP)'}
            </label>
            <input
              type="number"
              value={previousArrearsAmount}
              onChange={(e) => setPreviousArrearsAmount(Number(e.target.value))}
              placeholder="0"
              style={{
                width: '100%',
                height: '40px',
                padding: '0 10px',
                borderRadius: '8px',
                border: '1.5px solid #cbd5e1',
                fontSize: '14px',
                fontWeight: 700,
                boxSizing: 'border-box',
              }}
            />
          </div>

          {/* Total Sum Display */}
          <div
            style={{
              marginTop: '12px',
              padding: '12px 14px',
              backgroundColor: '#eaf5ee',
              borderRadius: '10px',
              border: '1.5px solid #1b4332',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
            }}
          >
            <span style={{ fontSize: '13px', fontWeight: 800, color: '#1b4332' }}>
              {isAr ? 'إجمالي المبلغ المستلم:' : 'Total Collected Amount:'}
            </span>
            <span style={{ fontSize: '18px', fontWeight: 900, color: '#1b4332' }}>
              {formatCurrency(totalCalculated, language)}
            </span>
          </div>
        </div>

        {/* Date and Notes */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', marginBottom: '18px' }}>
          <div>
            <label style={{ display: 'block', fontSize: '11px', fontWeight: 700, color: '#475569', marginBottom: '3px' }}>
              {isAr ? 'تاريخ السداد والتحصيل' : 'Payment / Collection Date'}
            </label>
            <input
              type="date"
              value={paidDate}
              onChange={(e) => setPaidDate(e.target.value)}
              style={{
                width: '100%',
                height: '40px',
                padding: '0 10px',
                borderRadius: '8px',
                border: '1px solid #cbd5e1',
                fontSize: '13px',
                boxSizing: 'border-box',
              }}
            />
          </div>

          <div>
            <label style={{ display: 'block', fontSize: '11px', fontWeight: 700, color: '#475569', marginBottom: '3px' }}>
              {isAr ? 'ملاحظات إضافية' : 'Notes'}
            </label>
            <input
              type="text"
              value={note}
              onChange={(e) => setNote(e.target.value)}
              placeholder={isAr ? 'ملاحظات اختيارية للإيصال' : 'Optional receipt notes'}
              style={{
                width: '100%',
                height: '40px',
                padding: '0 10px',
                borderRadius: '8px',
                border: '1px solid #cbd5e1',
                fontSize: '13px',
                boxSizing: 'border-box',
              }}
            />
          </div>
        </div>

        {/* Actions */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
          {/* Print Receipt Button */}
          <button
            onClick={handlePrintReceipt}
            disabled={isGeneratingPDF}
            style={{
              width: '100%',
              padding: '12px',
              borderRadius: '12px',
              border: '1.5px solid #1b4332',
              backgroundColor: '#eaf5ee',
              color: '#1b4332',
              fontSize: '14px',
              fontWeight: 800,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '8px',
              cursor: isGeneratingPDF ? 'wait' : 'pointer',
            }}
          >
            <Printer size={18} />
            <span>
              {isGeneratingPDF
                ? (isAr ? 'جاري إنشاء الإيصال...' : 'Generating PDF...')
                : (isAr ? 'تحويل وطباعة إيصال السداد (PDF)' : 'Print & Download Receipt (PDF)')}
            </span>
          </button>

          {/* Confirm & Save Button */}
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
            <span>{isAr ? 'تأكيد وحفظ التحصيل' : 'Confirm & Save Collection'}</span>
          </button>

          {payment.status === 'paid' && (
            <button
              onClick={handleMarkPending}
              style={{
                width: '100%',
                padding: '9px',
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

          {/* Delete Payment option ("make anything i can add i can delete also") */}
          {!showDeleteConfirm ? (
            <button
              onClick={() => setShowDeleteConfirm(true)}
              style={{
                width: '100%',
                padding: '8px',
                borderRadius: '8px',
                border: 'none',
                background: 'transparent',
                color: '#94a3b8',
                fontSize: '11px',
                fontWeight: 600,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '4px',
                cursor: 'pointer',
              }}
            >
              <Trash2 size={14} />
              <span>{isAr ? 'حذف هذا القسط' : 'Delete this payment entry'}</span>
            </button>
          ) : (
            <div
              style={{
                padding: '8px',
                borderRadius: '8px',
                backgroundColor: '#fef2f2',
                border: '1px solid #fca5a5',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
              }}
            >
              <span style={{ fontSize: '11px', color: '#dc2626', fontWeight: 700 }}>
                {isAr ? 'هل أنت متأكد من حذف القسط؟' : 'Are you sure you want to delete?'}
              </span>
              <div style={{ display: 'flex', gap: '6px' }}>
                <button
                  onClick={handleDelete}
                  style={{
                    padding: '4px 8px',
                    borderRadius: '6px',
                    backgroundColor: '#dc2626',
                    color: '#fff',
                    border: 'none',
                    fontSize: '11px',
                    fontWeight: 700,
                    cursor: 'pointer',
                  }}
                >
                  {isAr ? 'نعم، حذف' : 'Yes, Delete'}
                </button>
                <button
                  onClick={() => setShowDeleteConfirm(false)}
                  style={{
                    padding: '4px 8px',
                    borderRadius: '6px',
                    backgroundColor: '#e2e8f0',
                    color: '#475569',
                    border: 'none',
                    fontSize: '11px',
                    fontWeight: 700,
                    cursor: 'pointer',
                  }}
                >
                  {isAr ? 'إلغاء' : 'Cancel'}
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
