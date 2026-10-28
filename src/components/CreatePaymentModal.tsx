import React, { useState, useMemo } from 'react';
import { useApp } from '../context/AppContext';
import { toISODate } from '../utils/date';
import { formatCurrency } from '../utils/number';
import { CheckCircle2, Calendar, FileText, X, Printer, Plus, User, Building } from 'lucide-react';
import { generateAndShareReceiptPDF } from '../services/pdfGenerator';
import { Payment } from '../types';

interface CreatePaymentModalProps {
  onClose: () => void;
  preselectedUnitId?: string;
  preselectedContractId?: string;
}

export const CreatePaymentModal: React.FC<CreatePaymentModalProps> = ({
  onClose,
  preselectedUnitId,
  preselectedContractId,
}) => {
  const {
    contracts,
    properties,
    units,
    tenants,
    settings,
    language,
    addPayment,
  } = useApp();
  const isAr = language === 'ar';

  // Active contracts list for selection
  const activeContracts = useMemo(() => {
    return contracts.filter((c) => !c.isHistory && c.status === 'active');
  }, [contracts]);

  // Initial contract ID selection
  const [selectedContractId, setSelectedContractId] = useState<string>(() => {
    if (preselectedContractId) return preselectedContractId;
    if (preselectedUnitId) {
      const match = contracts.find((c) => !c.isHistory && c.unitId === preselectedUnitId);
      if (match) return match.id;
    }
    return activeContracts[0]?.id || '';
  });

  const selectedContract = contracts.find((c) => c.id === selectedContractId);
  const selectedUnit = selectedContract ? units.find((u) => u.id === selectedContract.unitId) : undefined;
  const selectedProperty = selectedUnit ? properties.find((p) => p.id === selectedUnit.propertyId) : undefined;
  const linkedTenant = selectedContract?.tenantId
    ? tenants.find((t) => t.id === selectedContract.tenantId)
    : undefined;

  const tenantDisplayName =
    linkedTenant?.fullName || selectedContract?.tenantName || (isAr ? 'مستأجر غير محدد' : 'Unassigned Tenant');

  // Month-Year descriptor (e.g. 2026-10 or شهر أكتوبر 2026)
  const currentMonth = toISODate(new Date()).slice(0, 7);
  const [monthYear, setMonthYear] = useState<string>(currentMonth);
  const [dueDate, setDueDate] = useState<string>(toISODate(new Date()));
  const [paidDate, setPaidDate] = useState<string>(toISODate(new Date()));
  const [status, setStatus] = useState<'paid' | 'pending'>('paid');

  // Itemized amounts
  const [rentAmount, setRentAmount] = useState<number>(selectedContract?.monthlyRent || 0);
  const [electricityAmount, setElectricityAmount] = useState<number>(0);
  const [waterAmount, setWaterAmount] = useState<number>(0);
  const [stairsCleaningAmount, setStairsCleaningAmount] = useState<number>(0);
  const [previousArrearsAmount, setPreviousArrearsAmount] = useState<number>(0);
  const [note, setNote] = useState<string>(isAr ? 'تم التحصيل والسداد نقداً' : 'Collected in cash');
  const [isGeneratingPDF, setIsGeneratingPDF] = useState(false);

  // When selectedContract changes, update default rent
  const handleContractChange = (contractId: string) => {
    setSelectedContractId(contractId);
    const cnt = contracts.find((c) => c.id === contractId);
    if (cnt) {
      setRentAmount(cnt.monthlyRent);
    }
  };

  const totalCalculated =
    Number(rentAmount || 0) +
    Number(electricityAmount || 0) +
    Number(waterAmount || 0) +
    Number(stairsCleaningAmount || 0) +
    Number(previousArrearsAmount || 0);

  const handleSavePayment = async (printPDF: boolean = false) => {
    if (!selectedContract || !selectedUnit) {
      alert(isAr ? 'يرجى اختيار المستأجر والوحدة أولاً' : 'Please select tenant and unit');
      return;
    }

    const newPaymentData: Omit<Payment, 'id'> = {
      contractId: selectedContract.id,
      unitId: selectedUnit.id,
      dueDate,
      amount: totalCalculated,
      paidAmount: status === 'paid' ? totalCalculated : undefined,
      paidDate: status === 'paid' ? paidDate : null,
      status,
      monthYear,
      rentAmount: Number(rentAmount || 0),
      electricityAmount: Number(electricityAmount || 0),
      waterAmount: Number(waterAmount || 0),
      stairsCleaningAmount: Number(stairsCleaningAmount || 0),
      previousArrearsAmount: Number(previousArrearsAmount || 0),
      note: note.trim() || undefined,
    };

    const createdPayment = await addPayment(newPaymentData);

    if (printPDF) {
      setIsGeneratingPDF(true);
      try {
        await generateAndShareReceiptPDF({
          payment: createdPayment,
          tenantName: tenantDisplayName,
          tenantPhone: linkedTenant?.phone,
          propertyName: selectedProperty?.name || '',
          unitNumber: selectedUnit?.unitNumber || '',
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
          notes: note.trim(),
        });
      } catch (err) {
        console.error('PDF error:', err);
      } finally {
        setIsGeneratingPDF(false);
      }
    }

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
        alignItems: 'flex-end',
        justifyContent: 'center',
      }}
    >
      <div
        style={{
          backgroundColor: '#ffffff',
          borderTopLeftRadius: '24px',
          borderTopRightRadius: '24px',
          width: '100%',
          maxWidth: '560px',
          maxHeight: '92vh',
          display: 'flex',
          flexDirection: 'column',
          boxShadow: '0 -4px 24px rgba(0,0,0,0.18)',
          paddingBottom: 'calc(var(--safe-bottom) + 16px)',
        }}
      >
        {/* Header */}
        <div
          style={{
            padding: '16px 20px',
            borderBottom: '1px solid #e2e8f0',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <div
              style={{
                width: '36px',
                height: '36px',
                borderRadius: '10px',
                backgroundColor: '#dcfce7',
                color: '#15803d',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <Plus size={20} />
            </div>
            <div>
              <h3 style={{ fontSize: '18px', fontWeight: 800, margin: 0, color: '#1e293b' }}>
                {isAr ? 'إضافة دفعة / تحصيل جديد' : 'Add New Payment / Bill'}
              </h3>
              <p style={{ fontSize: '12px', color: '#64748b', margin: 0 }}>
                {isAr ? 'إنشاء فاتورة سداد مخصصة لأي مستأجر' : 'Create payment bill for any tenant'}
              </p>
            </div>
          </div>

          <button
            onClick={onClose}
            style={{
              width: '34px',
              height: '34px',
              borderRadius: '50%',
              backgroundColor: '#f1f5f9',
              border: 'none',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              cursor: 'pointer',
            }}
          >
            <X size={18} color="#64748b" />
          </button>
        </div>

        {/* Form Body */}
        <div style={{ padding: '20px', overflowY: 'auto', flex: 1, display: 'flex', flexDirection: 'column', gap: '16px' }}>
          {/* Tenant & Unit Select */}
          <div>
            <label style={{ display: 'block', fontSize: '12px', fontWeight: 800, color: '#334155', marginBottom: '6px' }}>
              {isAr ? 'اختر المستأجر والعقد / الوحدة' : 'Select Tenant & Lease / Unit'}
            </label>
            {activeContracts.length === 0 ? (
              <div style={{ padding: '12px', backgroundColor: '#fffbeb', borderRadius: '8px', border: '1px solid #fde68a', fontSize: '13px', color: '#b45309' }}>
                {isAr ? 'لا توجد عقود نشطة حالياً. يرجى إنشاء عقد أولاً.' : 'No active contracts. Please create a contract first.'}
              </div>
            ) : (
              <select
                value={selectedContractId}
                onChange={(e) => handleContractChange(e.target.value)}
                style={{
                  width: '100%',
                  height: '46px',
                  padding: '0 12px',
                  borderRadius: '10px',
                  border: '1.5px solid #1b4332',
                  fontSize: '14px',
                  fontWeight: 700,
                  backgroundColor: '#fcfdfd',
                  color: '#0f172a',
                }}
              >
                {activeContracts.map((cnt) => {
                  const u = units.find((un) => un.id === cnt.unitId);
                  const p = u ? properties.find((pr) => pr.id === u.propertyId) : undefined;
                  const t = cnt.tenantId ? tenants.find((tn) => tn.id === cnt.tenantId) : undefined;
                  const name = t?.fullName || cnt.tenantName;
                  return (
                    <option key={cnt.id} value={cnt.id}>
                      {name} — {p?.name || ''} (وحدة {u?.unitNumber || ''}) — إيجار: {cnt.monthlyRent} ج.م
                    </option>
                  );
                })}
              </select>
            )}
          </div>

          {/* Month & Dates */}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
            <div>
              <label style={{ display: 'block', fontSize: '12px', fontWeight: 700, color: '#475569', marginBottom: '4px' }}>
                {isAr ? 'الشهر المستحق' : 'Month / Period'}
              </label>
              <input
                type="month"
                value={monthYear}
                onChange={(e) => setMonthYear(e.target.value)}
                style={{
                  width: '100%',
                  height: '42px',
                  padding: '0 10px',
                  borderRadius: '8px',
                  border: '1px solid #cbd5e1',
                  fontSize: '13px',
                  boxSizing: 'border-box',
                }}
              />
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '12px', fontWeight: 700, color: '#475569', marginBottom: '4px' }}>
                {isAr ? 'حالة الفاتورة' : 'Bill Status'}
              </label>
              <select
                value={status}
                onChange={(e) => setStatus(e.target.value as any)}
                style={{
                  width: '100%',
                  height: '42px',
                  padding: '0 10px',
                  borderRadius: '8px',
                  border: '1px solid #cbd5e1',
                  fontSize: '13px',
                  fontWeight: 700,
                  backgroundColor: status === 'paid' ? '#ecfdf5' : '#fffbeb',
                  color: status === 'paid' ? '#047857' : '#b45309',
                }}
              >
                <option value="paid">{isAr ? 'مسددة / تم التحصيل' : 'Paid'}</option>
                <option value="pending">{isAr ? 'مستحقة / غير مسددة' : 'Pending / Due'}</option>
              </select>
            </div>
          </div>

          {/* Due date and Paid Date */}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
            <div>
              <label style={{ display: 'block', fontSize: '12px', fontWeight: 700, color: '#475569', marginBottom: '4px' }}>
                {isAr ? 'تاريخ الاستحقاق' : 'Due Date'}
              </label>
              <input
                type="date"
                value={dueDate}
                onChange={(e) => setDueDate(e.target.value)}
                style={{
                  width: '100%',
                  height: '42px',
                  padding: '0 10px',
                  borderRadius: '8px',
                  border: '1px solid #cbd5e1',
                  fontSize: '13px',
                  boxSizing: 'border-box',
                }}
              />
            </div>

            {status === 'paid' && (
              <div>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 700, color: '#475569', marginBottom: '4px' }}>
                  {isAr ? 'تاريخ السداد والتحصيل' : 'Paid Date'}
                </label>
                <input
                  type="date"
                  value={paidDate}
                  onChange={(e) => setPaidDate(e.target.value)}
                  style={{
                    width: '100%',
                    height: '42px',
                    padding: '0 10px',
                    borderRadius: '8px',
                    border: '1px solid #cbd5e1',
                    fontSize: '13px',
                    boxSizing: 'border-box',
                  }}
                />
              </div>
            )}
          </div>

          {/* Itemized Breakdown Box */}
          <div
            style={{
              backgroundColor: '#f8fafc',
              border: '1.5px solid #cbd5e1',
              borderRadius: '14px',
              padding: '16px',
            }}
          >
            <div style={{ fontSize: '14px', fontWeight: 800, color: '#1e293b', marginBottom: '12px', display: 'flex', alignItems: 'center', gap: '6px' }}>
              <FileText size={16} color="#1b4332" />
              <span>{isAr ? 'بنود التحصيل التفصيلية (الفاتورة)' : 'Itemized Breakdown'}</span>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
              {/* Rent */}
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '10px' }}>
                <label style={{ fontSize: '13px', fontWeight: 700, color: '#334155', flex: 1 }}>
                  {isAr ? 'قيمة الإيجار الشهري' : 'Rent Amount'}
                </label>
                <div style={{ width: '140px' }}>
                  <input
                    type="number"
                    min={0}
                    value={rentAmount}
                    onChange={(e) => setRentAmount(Number(e.target.value))}
                    style={{
                      width: '100%',
                      height: '38px',
                      padding: '0 10px',
                      borderRadius: '8px',
                      border: '1px solid #cbd5e1',
                      fontSize: '14px',
                      fontWeight: 700,
                      color: '#1b4332',
                      boxSizing: 'border-box',
                      textAlign: 'left',
                    }}
                  />
                </div>
              </div>

              {/* Electricity */}
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '10px' }}>
                <label style={{ fontSize: '13px', color: '#475569', flex: 1 }}>
                  {isAr ? 'فاتورة الكهرباء' : 'Electricity'}
                </label>
                <div style={{ width: '140px' }}>
                  <input
                    type="number"
                    min={0}
                    value={electricityAmount || ''}
                    placeholder="0"
                    onChange={(e) => setElectricityAmount(Number(e.target.value) || 0)}
                    style={{
                      width: '100%',
                      height: '38px',
                      padding: '0 10px',
                      borderRadius: '8px',
                      border: '1px solid #cbd5e1',
                      fontSize: '14px',
                      boxSizing: 'border-box',
                      textAlign: 'left',
                    }}
                  />
                </div>
              </div>

              {/* Water */}
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '10px' }}>
                <label style={{ fontSize: '13px', color: '#475569', flex: 1 }}>
                  {isAr ? 'فاتورة المياه' : 'Water'}
                </label>
                <div style={{ width: '140px' }}>
                  <input
                    type="number"
                    min={0}
                    value={waterAmount || ''}
                    placeholder="0"
                    onChange={(e) => setWaterAmount(Number(e.target.value) || 0)}
                    style={{
                      width: '100%',
                      height: '38px',
                      padding: '0 10px',
                      borderRadius: '8px',
                      border: '1px solid #cbd5e1',
                      fontSize: '14px',
                      boxSizing: 'border-box',
                      textAlign: 'left',
                    }}
                  />
                </div>
              </div>

              {/* Stairs Cleaning */}
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '10px' }}>
                <label style={{ fontSize: '13px', color: '#475569', flex: 1 }}>
                  {isAr ? 'نظافة السلم والصيانة' : 'Stairs Cleaning'}
                </label>
                <div style={{ width: '140px' }}>
                  <input
                    type="number"
                    min={0}
                    value={stairsCleaningAmount || ''}
                    placeholder="0"
                    onChange={(e) => setStairsCleaningAmount(Number(e.target.value) || 0)}
                    style={{
                      width: '100%',
                      height: '38px',
                      padding: '0 10px',
                      borderRadius: '8px',
                      border: '1px solid #cbd5e1',
                      fontSize: '14px',
                      boxSizing: 'border-box',
                      textAlign: 'left',
                    }}
                  />
                </div>
              </div>

              {/* Previous Arrears */}
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '10px' }}>
                <label style={{ fontSize: '13px', color: '#b91c1c', fontWeight: 600, flex: 1 }}>
                  {isAr ? 'متأخرات ومبالغ سابقة' : 'Previous Unpaid Arrears'}
                </label>
                <div style={{ width: '140px' }}>
                  <input
                    type="number"
                    min={0}
                    value={previousArrearsAmount || ''}
                    placeholder="0"
                    onChange={(e) => setPreviousArrearsAmount(Number(e.target.value) || 0)}
                    style={{
                      width: '100%',
                      height: '38px',
                      padding: '0 10px',
                      borderRadius: '8px',
                      border: '1px solid #fca5a5',
                      backgroundColor: '#fef2f2',
                      fontSize: '14px',
                      fontWeight: 700,
                      color: '#b91c1c',
                      boxSizing: 'border-box',
                      textAlign: 'left',
                    }}
                  />
                </div>
              </div>

              {/* Total Display */}
              <div
                style={{
                  marginTop: '10px',
                  paddingTop: '12px',
                  borderTop: '2px dashed #cbd5e1',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                }}
              >
                <span style={{ fontSize: '15px', fontWeight: 800, color: '#1e293b' }}>
                  {isAr ? 'الإجمالي المطلوب سداده:' : 'Total Amount Due:'}
                </span>
                <span style={{ fontSize: '20px', fontWeight: 900, color: '#1b4332' }}>
                  {formatCurrency(totalCalculated, language)}
                </span>
              </div>
            </div>
          </div>

          {/* Notes */}
          <div>
            <label style={{ display: 'block', fontSize: '12px', fontWeight: 700, color: '#475569', marginBottom: '4px' }}>
              {isAr ? 'ملاحظات وسند الاستلام' : 'Receipt Notes'}
            </label>
            <input
              type="text"
              value={note}
              onChange={(e) => setNote(e.target.value)}
              placeholder={isAr ? 'مثال: تم السداد نقداً شامل الخدمات' : 'e.g. Paid in cash...'}
              style={{
                width: '100%',
                height: '42px',
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
        <div
          style={{
            padding: '16px 20px',
            borderTop: '1px solid #e2e8f0',
            display: 'flex',
            flexDirection: 'column',
            gap: '8px',
          }}
        >
          {status === 'paid' && (
            <button
              onClick={() => handleSavePayment(true)}
              disabled={isGeneratingPDF}
              style={{
                backgroundColor: '#1b4332',
                color: '#ffffff',
                border: 'none',
                borderRadius: '12px',
                padding: '13px 16px',
                fontSize: '14px',
                fontWeight: 800,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '8px',
                cursor: 'pointer',
              }}
            >
              <Printer size={18} />
              <span>
                {isGeneratingPDF
                  ? (isAr ? 'جارٍ إصدار الإيصال...' : 'Generating...')
                  : (isAr ? 'حفظ وطباعة إيصال السداد (PDF)' : 'Save & Print PDF Receipt')}
              </span>
            </button>
          )}

          <div style={{ display: 'flex', gap: '10px' }}>
            <button
              onClick={onClose}
              style={{
                flex: 1,
                padding: '12px',
                borderRadius: '10px',
                border: '1px solid #cbd5e1',
                backgroundColor: '#ffffff',
                color: '#475569',
                fontSize: '13px',
                fontWeight: 700,
                cursor: 'pointer',
              }}
            >
              {isAr ? 'إلغاء' : 'Cancel'}
            </button>
            <button
              onClick={() => handleSavePayment(false)}
              style={{
                flex: 2,
                backgroundColor: status === 'paid' ? '#047857' : '#2563eb',
                color: '#ffffff',
                border: 'none',
                borderRadius: '10px',
                padding: '12px',
                fontSize: '13px',
                fontWeight: 800,
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '6px',
              }}
            >
              <CheckCircle2 size={16} />
              <span>{isAr ? 'حفظ الفاتورة في السجل' : 'Save Payment to Ledger'}</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
