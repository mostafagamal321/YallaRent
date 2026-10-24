import React, { useState } from 'react';
import { useApp } from '../context/AppContext';
import { Contract, Unit } from '../types';
import { formatCurrency } from '../utils/number';
import { formatUIDate, calculateDurationText, getDaysFromToday, toISODate } from '../utils/date';
import { generateAndShareContractPDF } from '../services/pdfGenerator';
import {
  X,
  FileText,
  Calendar,
  CheckCircle2,
  Clock,
  AlertTriangle,
  RotateCcw,
  Edit2,
  Share2,
  History,
  Trash2,
  User,
  Phone,
  Building,
} from 'lucide-react';

interface UnitDetailModalProps {
  unitId: string;
  onClose: () => void;
  onEditContract: (contract: Contract) => void;
  onOpenPaymentModal: (paymentId: string) => void;
}

export const UnitDetailModal: React.FC<UnitDetailModalProps> = ({
  unitId,
  onClose,
  onEditContract,
  onOpenPaymentModal,
}) => {
  const {
    units,
    properties,
    contracts,
    payments,
    settings,
    language,
    endContract,
    deleteContract,
    renewContract,
    markPaymentPaid,
  } = useApp();

  const isAr = language === 'ar';
  const unit = units.find((u) => u.id === unitId);
  const property = unit ? properties.find((p) => p.id === unit.propertyId) : undefined;

  // Active contract for this unit
  const activeContract = contracts.find((c) => !c.isHistory && c.unitId === unitId);

  // Past contracts history for this unit
  const historyContracts = contracts.filter((c) => c.isHistory && c.unitId === unitId);

  // Payments for this unit
  const unitPayments = payments
    .filter((p) => p.unitId === unitId)
    .sort((a, b) => b.dueDate.localeCompare(a.dueDate));

  // Renewal state
  const [showRenewModal, setShowRenewModal] = useState(false);
  const [newStartDate, setNewStartDate] = useState('');
  const [newEndDate, setNewEndDate] = useState('');
  const [newRent, setNewRent] = useState<number>(activeContract?.monthlyRent || 0);

  if (!unit || !property) return null;

  const handleStartRenew = () => {
    if (activeContract) {
      // Calculate start date: day after old contract ends
      const oldEnd = new Date(activeContract.endDate);
      const nextStart = new Date(oldEnd.getTime() + 24 * 60 * 60 * 1000);
      const nextEnd = new Date(nextStart.getFullYear() + 1, nextStart.getMonth(), nextStart.getDate() - 1);
      
      setNewStartDate(toISODate(nextStart));
      setNewEndDate(toISODate(nextEnd));
      setNewRent(activeContract.monthlyRent);
      setShowRenewModal(true);
    }
  };

  const handleConfirmRenew = async () => {
    if (!activeContract || !newStartDate || !newEndDate) return;
    await renewContract(activeContract.id, newStartDate, newEndDate, newRent);
    setShowRenewModal(false);
  };

  const handlePrintPDF = async () => {
    if (!activeContract) return;
    const isShop = property.type === 'shops';
    const template = isShop ? settings.templates.shops : settings.templates.apartments;
    await generateAndShareContractPDF({
      contract: activeContract,
      unit,
      property,
      landlord: settings.landlord,
      templateText: template,
    });
  };

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        backgroundColor: 'rgba(0,0,0,0.6)',
        zIndex: 200,
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'flex-end',
      }}
    >
      <div
        style={{
          backgroundColor: '#ffffff',
          borderTopLeftRadius: '24px',
          borderTopRightRadius: '24px',
          maxHeight: '90vh',
          display: 'flex',
          flexDirection: 'column',
          boxShadow: '0 -4px 20px rgba(0,0,0,0.15)',
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
          <div>
            <h3 style={{ fontSize: '18px', fontWeight: 800, color: '#1e293b', margin: 0 }}>
              {isAr ? `وحدة رقم ${unit.unitNumber}` : `Unit #${unit.unitNumber}`}
            </h3>
            <p style={{ fontSize: '12px', color: '#64748b', margin: 0 }}>
              {property.name} • {unit.description}
            </p>
          </div>

          <button
            onClick={onClose}
            style={{
              width: '36px',
              height: '36px',
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

        {/* Scrollable Content */}
        <div style={{ padding: '20px', overflowY: 'auto', flex: 1, display: 'flex', flexDirection: 'column', gap: '18px' }}>
          {/* Active Contract Card */}
          {activeContract ? (
            <div
              style={{
                backgroundColor: '#f8fafc',
                border: '1px solid #cbd5e1',
                borderRadius: '16px',
                padding: '16px',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '10px' }}>
                <span
                  style={{
                    backgroundColor: '#dcfce7',
                    color: '#15803d',
                    fontSize: '11px',
                    fontWeight: 800,
                    padding: '3px 10px',
                    borderRadius: '12px',
                  }}
                >
                  {isAr ? 'العقد الساري حالياً' : 'Active Contract'}
                </span>
                <span style={{ fontSize: '12px', color: '#64748b' }}>
                  {isAr ? 'تاريخ التحرير:' : 'Contract date:'} {formatUIDate(activeContract.contractDate || activeContract.startDate)}
                </span>
              </div>

              <div style={{ fontSize: '17px', fontWeight: 800, color: '#0f172a', marginBottom: '6px' }}>
                {activeContract.tenantName}
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: '10px', marginBottom: '14px', fontSize: '13px' }}>
                <div>
                  <span style={{ color: '#64748b' }}>{isAr ? 'الإيجار الشهري:' : 'Monthly Rent:'}</span>{' '}
                  <strong style={{ color: '#1b4332' }}>{formatCurrency(activeContract.monthlyRent, language)}</strong>
                </div>
                <div>
                  <span style={{ color: '#64748b' }}>{isAr ? 'يوم الاستحقاق:' : 'Due Day:'}</span>{' '}
                  <strong>{activeContract.rentDueDay} {isAr ? 'من كل شهر' : 'each month'}</strong>
                </div>
                <div style={{ gridColumn: 'span 2' }}>
                  <span style={{ color: '#64748b' }}>{isAr ? 'مدة العقد:' : 'Duration:'}</span>{' '}
                  <strong>{calculateDurationText(activeContract.startDate, activeContract.endDate, language)}</strong> ({formatUIDate(activeContract.startDate)} - {formatUIDate(activeContract.endDate)})
                </div>
                {activeContract.notes && (
                  <div style={{ gridColumn: 'span 2', color: '#0284c7', fontSize: '12px' }}>
                    <strong>{isAr ? 'ملاحظة:' : 'Note:'}</strong> {activeContract.notes}
                  </div>
                )}
              </div>

              {/* Action Buttons */}
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: '8px' }}>
                <button
                  onClick={handlePrintPDF}
                  style={{
                    backgroundColor: '#1b4332',
                    color: '#ffffff',
                    border: 'none',
                    borderRadius: '10px',
                    padding: '10px',
                    fontSize: '12px',
                    fontWeight: 700,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    gap: '6px',
                    cursor: 'pointer',
                  }}
                >
                  <Share2 size={15} />
                  <span>{isAr ? 'مشاركة العقد PDF' : 'Share PDF'}</span>
                </button>

                <button
                  onClick={handleStartRenew}
                  style={{
                    backgroundColor: '#0284c7',
                    color: '#ffffff',
                    border: 'none',
                    borderRadius: '10px',
                    padding: '10px',
                    fontSize: '12px',
                    fontWeight: 700,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    gap: '6px',
                    cursor: 'pointer',
                  }}
                >
                  <RotateCcw size={15} />
                  <span>{isAr ? 'تجديد العقد' : 'Renew Lease'}</span>
                </button>

                <button
                  onClick={() => onEditContract(activeContract)}
                  style={{
                    backgroundColor: '#ffffff',
                    color: '#334155',
                    border: '1px solid #cbd5e1',
                    borderRadius: '10px',
                    padding: '10px',
                    fontSize: '12px',
                    fontWeight: 700,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    gap: '6px',
                    cursor: 'pointer',
                  }}
                >
                  <Edit2 size={15} />
                  <span>{isAr ? 'تعديل البيانات' : 'Edit Details'}</span>
                </button>

                <button
                  onClick={async () => {
                    if (confirm(isAr ? 'هل أنت متأكد من إنهاء هذا العقد ونقله للأرشيف؟' : 'End this contract and archive?')) {
                      await endContract(activeContract.id);
                    }
                  }}
                  style={{
                    backgroundColor: '#fef2f2',
                    color: '#dc2626',
                    border: '1px solid #fecaca',
                    borderRadius: '10px',
                    padding: '10px',
                    fontSize: '12px',
                    fontWeight: 700,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    gap: '6px',
                    cursor: 'pointer',
                  }}
                >
                  <Clock size={15} />
                  <span>{isAr ? 'إنهاء العقد' : 'End Lease'}</span>
                </button>
              </div>
            </div>
          ) : (
            <div
              style={{
                backgroundColor: '#f8fafc',
                borderRadius: '16px',
                padding: '24px',
                textAlign: 'center',
                border: '1px dashed #cbd5e1',
              }}
            >
              <div style={{ fontSize: '15px', fontWeight: 700, color: '#64748b', marginBottom: '8px' }}>
                {isAr ? 'الوحدة شاغرة حالياً' : 'Unit is currently vacant'}
              </div>
              {unit.notes && (
                <div style={{ fontSize: '13px', color: '#0369a1', marginBottom: '14px' }}>
                  {unit.notes}
                </div>
              )}
            </div>
          )}

          {/* Payment History for this Unit */}
          <div>
            <h4 style={{ fontSize: '15px', fontWeight: 800, color: '#1e293b', marginBottom: '10px' }}>
              {isAr ? 'سجل دفعات الإيجار' : 'Payment History'} ({unitPayments.length})
            </h4>

            {unitPayments.length === 0 ? (
              <div style={{ color: '#94a3b8', fontSize: '13px', textAlign: 'center', padding: '16px' }}>
                {isAr ? 'لا توجد دفعات مسجلة بعد' : 'No payments recorded yet'}
              </div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                {unitPayments.map((p) => {
                  const isPaid = p.status === 'paid';
                  const isOverdue = p.status === 'overdue';

                  return (
                    <div
                      key={p.id}
                      onClick={() => onOpenPaymentModal(p.id)}
                      style={{
                        padding: '12px 14px',
                        borderRadius: '10px',
                        border: '1px solid #e2e8f0',
                        backgroundColor: '#ffffff',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        cursor: 'pointer',
                      }}
                    >
                      <div>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '2px' }}>
                          <span
                            style={{
                              fontSize: '11px',
                              fontWeight: 800,
                              padding: '2px 8px',
                              borderRadius: '4px',
                              backgroundColor: isPaid ? '#dcfce7' : isOverdue ? '#fee2e2' : '#fef3c7',
                              color: isPaid ? '#15803d' : isOverdue ? '#b91c1c' : '#b45309',
                            }}
                          >
                            {isPaid ? (isAr ? 'تم السداد' : 'Paid') : isOverdue ? (isAr ? 'متأخر' : 'Overdue') : (isAr ? 'مستحق' : 'Pending')}
                          </span>
                          <span style={{ fontSize: '13px', fontWeight: 700, color: '#334155' }}>
                            {formatUIDate(p.dueDate)}
                          </span>
                        </div>
                        {p.paidDate && (
                          <div style={{ fontSize: '11px', color: '#15803d' }}>
                            {isAr ? `تاريخ السداد: ${formatUIDate(p.paidDate)}` : `Paid on: ${formatUIDate(p.paidDate)}`}
                          </div>
                        )}
                        {p.note && <div style={{ fontSize: '11px', color: '#64748b' }}>{p.note}</div>}
                      </div>

                      <div style={{ textAlign: isAr ? 'left' : 'right' }}>
                        <div style={{ fontSize: '15px', fontWeight: 800, color: '#1e293b' }}>
                          {formatCurrency(p.amount, language)}
                        </div>
                        {!isPaid && (
                          <span style={{ fontSize: '11px', color: '#1b4332', fontWeight: 700 }}>
                            {isAr ? 'تسجيل السداد' : 'Pay'}
                          </span>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>

          {/* Past Contracts History */}
          {historyContracts.length > 0 && (
            <div>
              <h4 style={{ fontSize: '15px', fontWeight: 800, color: '#475569', marginBottom: '10px' }}>
                {isAr ? 'أرشيف العقود السابقة للوحدة' : 'Previous Contracts Archive'} ({historyContracts.length})
              </h4>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                {historyContracts.map((hc) => (
                  <div
                    key={hc.id}
                    style={{
                      padding: '12px 14px',
                      borderRadius: '10px',
                      border: '1px dashed #cbd5e1',
                      backgroundColor: '#f8fafc',
                      fontSize: '12px',
                    }}
                  >
                    <div style={{ fontWeight: 800, color: '#334155', marginBottom: '2px' }}>
                      {hc.tenantName}
                    </div>
                    <div style={{ color: '#64748b' }}>
                      {formatUIDate(hc.startDate)} - {formatUIDate(hc.endDate)} • {formatCurrency(hc.monthlyRent, language)}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Renewal Dialog */}
      {showRenewModal && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            backgroundColor: 'rgba(0,0,0,0.6)',
            zIndex: 300,
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
              maxWidth: '400px',
              width: '100%',
              boxShadow: '0 10px 25px rgba(0,0,0,0.2)',
            }}
          >
            <h4 style={{ fontSize: '17px', fontWeight: 800, color: '#1e293b', marginBottom: '14px' }}>
              {isAr ? 'تجديد عقد الإيجار' : 'Renew Lease Contract'}
            </h4>
            <p style={{ fontSize: '12px', color: '#64748b', marginBottom: '16px' }}>
              {isAr
                ? 'سيتم الاحتفاظ بالعقد القديم في السجل وإنشاء فترة تعاقد جديدة بنفس بيانات المستأجر.'
                : 'The previous contract will be archived and a new lease period will be created.'}
            </p>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', marginBottom: '20px' }}>
              <div>
                <label style={{ display: 'block', fontSize: '11px', fontWeight: 700, color: '#475569', marginBottom: '2px' }}>
                  {isAr ? 'تاريخ بداية العقد الجديد' : 'New Start Date'}
                </label>
                <input
                  type="date"
                  value={newStartDate}
                  onChange={(e) => setNewStartDate(e.target.value)}
                  style={{ width: '100%', height: '42px', padding: '0 10px', borderRadius: '8px', border: '1px solid #cbd5e1', fontSize: '13px', boxSizing: 'border-box' }}
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '11px', fontWeight: 700, color: '#475569', marginBottom: '2px' }}>
                  {isAr ? 'تاريخ نهاية العقد الجديد' : 'New End Date'}
                </label>
                <input
                  type="date"
                  value={newEndDate}
                  onChange={(e) => setNewEndDate(e.target.value)}
                  style={{ width: '100%', height: '42px', padding: '0 10px', borderRadius: '8px', border: '1px solid #cbd5e1', fontSize: '13px', boxSizing: 'border-box' }}
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '11px', fontWeight: 700, color: '#475569', marginBottom: '2px' }}>
                  {isAr ? 'قيمة الإيجار الشهري الجديد (ج.م)' : 'New Monthly Rent (EGP)'}
                </label>
                <input
                  type="number"
                  value={newRent}
                  onChange={(e) => setNewRent(Number(e.target.value))}
                  style={{ width: '100%', height: '42px', padding: '0 10px', borderRadius: '8px', border: '1px solid #cbd5e1', fontSize: '14px', fontWeight: 700, boxSizing: 'border-box' }}
                />
              </div>
            </div>

            <div style={{ display: 'flex', gap: '10px' }}>
              <button
                onClick={() => setShowRenewModal(false)}
                style={{ flex: 1, padding: '10px', borderRadius: '10px', border: '1px solid #cbd5e1', backgroundColor: '#ffffff', color: '#475569', fontWeight: 700, cursor: 'pointer' }}
              >
                {isAr ? 'إلغاء' : 'Cancel'}
              </button>
              <button
                onClick={handleConfirmRenew}
                style={{ flex: 2, padding: '10px', borderRadius: '10px', border: 'none', backgroundColor: '#0284c7', color: '#ffffff', fontWeight: 700, cursor: 'pointer' }}
              >
                {isAr ? 'تأكيد التجديد' : 'Confirm Renewal'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
