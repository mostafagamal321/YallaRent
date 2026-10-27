import React, { useState } from 'react';
import { useApp } from '../context/AppContext';
import { Tenant, Contract, Payment } from '../types';
import { formatUIDate, calculateDurationText, toISODate } from '../utils/date';
import { formatCurrency, toArabicIndicDigits } from '../utils/number';
import { generateAndShareContractPDF } from '../services/pdfGenerator';
import {
  User,
  Phone,
  MessageCircle,
  Eye,
  EyeOff,
  Edit,
  Printer,
  FileText,
  Trash2,
  X,
  CreditCard,
  Building,
  Home,
  CheckCircle,
  Clock,
  AlertTriangle,
  Lock,
} from 'lucide-react';

interface TenantProfileModalProps {
  tenantId: string;
  onClose: () => void;
  onEdit: (tenant: Tenant) => void;
  onOpenContract?: (contract: Contract) => void;
}

export const TenantProfileModal: React.FC<TenantProfileModalProps> = ({
  tenantId,
  onClose,
  onEdit,
  onOpenContract,
}) => {
  const {
    tenants,
    contracts,
    payments,
    properties,
    units,
    settings,
    language,
    deleteTenant,
  } = useApp();
  const isAr = language === 'ar';

  const tenant = tenants.find((t) => t.id === tenantId);
  const [showFullNationalId, setShowFullNationalId] = useState(false);
  const [showIdDocsSection, setShowIdDocsSection] = useState(false);
  const [pinInput, setPinInput] = useState('');
  const [isPinVerified, setIsPinVerified] = useState(!settings.pinEnabled);
  const [pinError, setPinError] = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [isPrintingContract, setIsPrintingContract] = useState(false);

  if (!tenant) return null;

  // Contracts for this tenant
  const tenantContracts = contracts.filter((c) => c.tenantId === tenant.id || c.tenantName === tenant.fullName);
  const activeContract = tenantContracts.find((c) => !c.isHistory && (c.status === 'active' || c.status === 'expiring_soon')) || tenantContracts[0];

  // Property & Unit info for active contract
  const activeUnit = activeContract ? units.find((u) => u.id === activeContract.unitId) : undefined;
  const activeProperty = activeUnit ? properties.find((p) => p.id === activeUnit.propertyId) : undefined;

  // Payments for this tenant's contracts
  const contractIds = new Set(tenantContracts.map((c) => c.id));
  const tenantPayments = payments
    .filter((p) => contractIds.has(p.contractId))
    .sort((a, b) => b.dueDate.localeCompare(a.dueDate));

  // Payment status summary
  const paidPayments = tenantPayments.filter((p) => p.status === 'paid');
  const lastPayment = paidPayments.length > 0 ? paidPayments[0] : null;

  const today = toISODate(new Date());
  const pendingPayments = tenantPayments.filter((p) => p.status !== 'paid');
  const nextDuePayment = pendingPayments
    .filter((p) => p.dueDate >= today)
    .sort((a, b) => a.dueDate.localeCompare(b.dueDate))[0];

  const totalOverdueAmount = tenantPayments
    .filter((p) => p.status === 'overdue')
    .reduce((sum, p) => sum + (p.amount - (p.paidAmount || 0)), 0);

  // Masked National ID: e.g. ٠٠٠٠٠٠٠٠٠٠٣١ (first 10 digits masked, last 4 digits visible)
  const formatMaskedNationalId = (idStr: string) => {
    if (!idStr) return isAr ? 'غير مسجل' : 'Not recorded';
    if (showFullNationalId) return toArabicIndicDigits(idStr);
    if (idStr.length <= 4) return '••••';
    const masked = '••••••••••' + idStr.slice(-4);
    return isAr ? toArabicIndicDigits(masked) : masked;
  };

  // WhatsApp click handler
  const handleWhatsApp = () => {
    if (!tenant.phone) return;
    let clean = tenant.phone.replace(/\D/g, '');
    if (clean.startsWith('0')) {
      clean = '2' + clean;
    }
    window.open(`https://wa.me/${clean}`, '_blank');
  };

  // Call click handler
  const handleCall = () => {
    if (!tenant.phone) return;
    window.open(`tel:${tenant.phone}`, '_self');
  };

  // Verify PIN for viewing ID documents
  const handleVerifyPin = () => {
    if (pinInput === settings.pinCode) {
      setIsPinVerified(true);
      setPinError(false);
    } else {
      setPinError(true);
    }
  };

  // Print Contract PDF
  const handlePrintContract = async () => {
    if (!activeContract || !activeUnit || !activeProperty) {
      alert(isAr ? 'لا يوجد عقد نشط ومكتمل لهذا المستأجر' : 'No active contract for this tenant');
      return;
    }
    setIsPrintingContract(true);
    try {
      const isShop = activeProperty.type === 'shops';
      const templateText = isShop ? settings.templates.shops : settings.templates.apartments;
      await generateAndShareContractPDF({
        contract: activeContract,
        unit: activeUnit,
        property: activeProperty,
        landlord: settings.landlord,
        templateText,
        tenant,
      });
    } catch (e) {
      console.error('Print contract error:', e);
    } finally {
      setIsPrintingContract(false);
    }
  };

  const handleDeleteTenant = async () => {
    await deleteTenant(tenant.id);
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
          borderRadius: '24px',
          padding: '24px',
          maxWidth: '520px',
          width: '100%',
          maxHeight: '94vh',
          overflowY: 'auto',
          boxShadow: '0 16px 36px rgba(0,0,0,0.28)',
        }}
      >
        {/* Top Bar */}
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '14px' }}>
          <div style={{ fontSize: '13px', fontWeight: 800, color: '#1b4332' }}>
            {isAr ? 'ملف المستأجر' : 'Tenant Profile'}
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

        {/* 1. TOP CARD */}
        <div
          style={{
            backgroundColor: '#fbfcf8',
            borderRadius: '20px',
            border: '1.5px solid #e2e8f0',
            padding: '20px',
            marginBottom: '20px',
            boxShadow: '0 4px 12px rgba(0,0,0,0.03)',
          }}
        >
          {/* Avatar + Name + National ID */}
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', textAlign: 'center', marginBottom: '16px' }}>
            <div
              style={{
                width: '100px',
                height: '100px',
                borderRadius: '50%',
                backgroundColor: '#e2e8f0',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                overflow: 'hidden',
                border: '4px solid #1b4332',
                marginBottom: '10px',
                boxShadow: '0 4px 10px rgba(27,67,50,0.15)',
              }}
            >
              {tenant.photoPath ? (
                <img
                  src={tenant.photoPath}
                  alt={tenant.fullName}
                  style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                />
              ) : (
                <User size={50} color="#94a3b8" />
              )}
            </div>

            <h2 style={{ fontSize: '20px', fontWeight: 800, color: '#1e293b', margin: '0 0 4px 0' }}>
              {tenant.fullName}
            </h2>

            {/* National ID with toggle */}
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                backgroundColor: '#f1f5f9',
                padding: '4px 12px',
                borderRadius: '20px',
                fontSize: '13px',
                fontWeight: 700,
                color: '#475569',
                marginTop: '4px',
              }}
            >
              <span>{isAr ? 'الرقم القومي:' : 'National ID:'}</span>
              <span style={{ letterSpacing: '1px', fontFamily: 'monospace' }}>
                {formatMaskedNationalId(tenant.nationalId)}
              </span>
              {tenant.nationalId && (
                <button
                  type="button"
                  onClick={() => setShowFullNationalId(!showFullNationalId)}
                  title={showFullNationalId ? (isAr ? 'إخفاء' : 'Hide') : (isAr ? 'إظهار' : 'Show')}
                  style={{ background: 'none', border: 'none', cursor: 'pointer', padding: 0, display: 'flex' }}
                >
                  {showFullNationalId ? <EyeOff size={15} color="#0284c7" /> : <Eye size={15} color="#0284c7" />}
                </button>
              )}
            </div>

            {/* Job & Nationality */}
            <div style={{ fontSize: '13px', color: '#64748b', marginTop: '6px' }}>
              <strong>{tenant.job || (isAr ? 'موظف' : 'Employee')}</strong> • {tenant.nationality || 'مصري'} • {tenant.address || 'الفيوم'}
            </div>
          </div>

          {/* Phone with WhatsApp and Call Buttons */}
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              backgroundColor: '#ffffff',
              border: '1px solid #e2e8f0',
              borderRadius: '12px',
              padding: '10px 14px',
              marginBottom: '14px',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Phone size={16} color="#1b4332" />
              <span style={{ fontSize: '14px', fontWeight: 700, color: '#1e293b', direction: 'ltr' }}>
                {tenant.phone || (isAr ? 'لا يوجد هاتف مسجل' : 'No phone recorded')}
              </span>
            </div>

            {tenant.phone && (
              <div style={{ display: 'flex', gap: '8px' }}>
                <button
                  onClick={handleWhatsApp}
                  style={{
                    padding: '6px 12px',
                    borderRadius: '8px',
                    backgroundColor: '#25D366',
                    color: '#ffffff',
                    border: 'none',
                    fontSize: '12px',
                    fontWeight: 800,
                    display: 'flex',
                    alignItems: 'center',
                    gap: '4px',
                    cursor: 'pointer',
                  }}
                >
                  <MessageCircle size={14} />
                  <span>{isAr ? 'واتساب' : 'WhatsApp'}</span>
                </button>
                <button
                  onClick={handleCall}
                  style={{
                    padding: '6px 12px',
                    borderRadius: '8px',
                    backgroundColor: '#1b4332',
                    color: '#ffffff',
                    border: 'none',
                    fontSize: '12px',
                    fontWeight: 800,
                    display: 'flex',
                    alignItems: 'center',
                    gap: '4px',
                    cursor: 'pointer',
                  }}
                >
                  <Phone size={14} />
                  <span>{isAr ? 'اتصال' : 'Call'}</span>
                </button>
              </div>
            )}
          </div>

          {/* Building & Unit */}
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              backgroundColor: '#f8fafc',
              border: '1px solid #e2e8f0',
              borderRadius: '12px',
              padding: '10px 14px',
              marginBottom: '14px',
              fontSize: '13px',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
              <Building size={16} color="#0284c7" />
              <span>{isAr ? 'العمارة:' : 'Building:'} <strong>{activeProperty?.name || (isAr ? 'غير محدد' : 'N/A')}</strong></span>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
              <Home size={16} color="#0284c7" />
              <span>{isAr ? 'رقم الوحدة:' : 'Unit:'} <strong>{activeUnit ? `وحدة ${activeUnit.unitNumber}` : (isAr ? 'غير محدد' : 'N/A')}</strong></span>
            </div>
          </div>

          {/* Current Contract Details */}
          {activeContract ? (
            <div
              style={{
                backgroundColor: '#ffffff',
                border: '1px solid #cbd5e1',
                borderRadius: '14px',
                padding: '12px 14px',
                marginBottom: '14px',
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
                <span style={{ fontSize: '13px', fontWeight: 800, color: '#1b4332' }}>
                  {isAr ? 'العقد الحالي' : 'Current Contract'}
                </span>
                <span
                  style={{
                    padding: '3px 10px',
                    borderRadius: '12px',
                    fontSize: '11px',
                    fontWeight: 800,
                    backgroundColor: activeContract.status === 'active' ? '#dcfce7' : '#fef3c7',
                    color: activeContract.status === 'active' ? '#166534' : '#92400e',
                  }}
                >
                  {activeContract.status === 'active' ? (isAr ? 'ساري' : 'Active') : (isAr ? 'أوشك على الانتهاء' : 'Expiring')}
                </span>
              </div>

              <div style={{ fontSize: '12px', color: '#475569', display: 'flex', flexDirection: 'column', gap: '4px' }}>
                <div>
                  {isAr ? 'الفترة:' : 'Period:'} <strong>{formatUIDate(activeContract.startDate)}</strong> {isAr ? 'إلى' : 'to'} <strong>{formatUIDate(activeContract.endDate)}</strong>
                </div>
                <div>
                  {isAr ? 'المدة المتبقية:' : 'Time remaining:'} <strong style={{ color: '#1b4332' }}>{calculateDurationText(today, activeContract.endDate, language)}</strong>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '4px', paddingTop: '6px', borderTop: '1px dashed #e2e8f0' }}>
                  <span>{isAr ? 'قيمة الإيجار الشهري:' : 'Monthly Rent:'} <strong style={{ color: '#1b4332', fontSize: '14px' }}>{formatCurrency(activeContract.monthlyRent, language)}</strong></span>
                  <span>{isAr ? 'يوم الاستحقاق:' : 'Due day:'} <strong>{activeContract.rentDueDay || 1} {isAr ? 'من كل شهر' : 'of month'}</strong></span>
                </div>
              </div>
            </div>
          ) : (
            <div style={{ padding: '10px', backgroundColor: '#f1f5f9', borderRadius: '10px', textAlign: 'center', fontSize: '12px', color: '#64748b', marginBottom: '14px' }}>
              {isAr ? 'لا يوجد عقد نشط حالياً لهذا المستأجر' : 'No active contract currently'}
            </div>
          )}

          {/* Payment Status Summary */}
          <div
            style={{
              backgroundColor: '#f0fdf4',
              borderRadius: '12px',
              border: '1px solid #bbf7d0',
              padding: '12px 14px',
              fontSize: '12px',
            }}
          >
            <div style={{ fontWeight: 800, color: '#166534', marginBottom: '6px', fontSize: '13px' }}>
              {isAr ? 'حالة السداد والتحصيل' : 'Payment Status'}
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '4px', color: '#14532d' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span>{isAr ? 'آخر سداد مسجل:' : 'Last payment:'}</span>
                <strong>{lastPayment ? `${formatUIDate(lastPayment.paidDate || lastPayment.dueDate)} (${formatCurrency(lastPayment.paidAmount || lastPayment.amount, language)})` : (isAr ? 'لا يوجد بعد' : 'None yet')}</strong>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span>{isAr ? 'الاستحقاق القادم:' : 'Next due date:'}</span>
                <strong>{nextDuePayment ? `${formatUIDate(nextDuePayment.dueDate)}` : (isAr ? 'لا يوجد' : 'None')}</strong>
              </div>
              {totalOverdueAmount > 0 && (
                <div style={{ display: 'flex', justifyContent: 'space-between', color: '#dc2626', fontWeight: 800, paddingTop: '4px', borderTop: '1px dashed #bbf7d0' }}>
                  <span>{isAr ? 'إجمالي المتأخرات المستحقة:' : 'Total overdue:'}</span>
                  <span>{formatCurrency(totalOverdueAmount, language)}</span>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* 2. OPTIONAL ID DOCUMENTS SECTION (PIN PROTECTED) */}
        <div style={{ border: '1px solid #e2e8f0', borderRadius: '16px', padding: '14px', marginBottom: '20px' }}>
          <button
            type="button"
            onClick={() => setShowIdDocsSection(!showIdDocsSection)}
            style={{
              width: '100%',
              background: 'none',
              border: 'none',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              cursor: 'pointer',
              padding: 0,
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <CreditCard size={18} color="#1b4332" />
              <span style={{ fontSize: '14px', fontWeight: 800, color: '#1b4332' }}>
                {isAr ? 'صور بطاقة الرقم القومي (وثائق الهوية)' : 'National ID Card Documents'}
              </span>
            </div>
            <span style={{ fontSize: '11px', color: '#64748b', fontWeight: 600 }}>
              {showIdDocsSection ? (isAr ? 'إخفاء' : 'Hide') : (isAr ? 'عرض' : 'Show')}
            </span>
          </button>

          {showIdDocsSection && (
            <div style={{ marginTop: '14px' }}>
              {settings.pinEnabled && !isPinVerified ? (
                <div style={{ padding: '14px', backgroundColor: '#f8fafc', borderRadius: '12px', textAlign: 'center' }}>
                  <Lock size={24} color="#64748b" style={{ margin: '0 auto 8px auto' }} />
                  <div style={{ fontSize: '12px', fontWeight: 700, color: '#334155', marginBottom: '8px' }}>
                    {isAr ? 'يرجى إدخال رمز الأمان (PIN) لعرض صور البطاقة' : 'Enter PIN to view ID documents'}
                  </div>
                  <div style={{ display: 'flex', gap: '8px', maxWidth: '240px', margin: '0 auto' }}>
                    <input
                      type="password"
                      maxLength={6}
                      value={pinInput}
                      onChange={(e) => setPinInput(e.target.value)}
                      placeholder="PIN"
                      style={{
                        flex: 1,
                        height: '38px',
                        textAlign: 'center',
                        fontSize: '16px',
                        letterSpacing: '4px',
                        borderRadius: '8px',
                        border: pinError ? '1.5px solid #dc2626' : '1.5px solid #cbd5e1',
                      }}
                    />
                    <button
                      type="button"
                      onClick={handleVerifyPin}
                      style={{
                        padding: '0 14px',
                        borderRadius: '8px',
                        backgroundColor: '#1b4332',
                        color: '#fff',
                        border: 'none',
                        fontWeight: 700,
                        fontSize: '12px',
                        cursor: 'pointer',
                      }}
                    >
                      {isAr ? 'تأكيد' : 'Verify'}
                    </button>
                  </div>
                  {pinError && (
                    <div style={{ fontSize: '11px', color: '#dc2626', marginTop: '6px', fontWeight: 700 }}>
                      {isAr ? 'رمز PIN غير صحيح' : 'Incorrect PIN'}
                    </div>
                  )}
                </div>
              ) : (
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                  {/* Front */}
                  <div style={{ border: '1px solid #e2e8f0', borderRadius: '10px', padding: '10px', textAlign: 'center' }}>
                    <div style={{ fontSize: '12px', fontWeight: 700, color: '#475569', marginBottom: '6px' }}>
                      {isAr ? 'وجه البطاقة الأمامي' : 'ID Front'}
                    </div>
                    {tenant.idCardPhotoFrontPath ? (
                      <img
                        src={tenant.idCardPhotoFrontPath}
                        alt="ID Front"
                        style={{ width: '100%', height: '110px', objectFit: 'contain', borderRadius: '6px', backgroundColor: '#f1f5f9' }}
                      />
                    ) : (
                      <div style={{ height: '90px', backgroundColor: '#f8fafc', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#94a3b8', fontSize: '11px', borderRadius: '6px' }}>
                        {isAr ? 'لا توجد صورة' : 'No photo'}
                      </div>
                    )}
                  </div>

                  {/* Back */}
                  <div style={{ border: '1px solid #e2e8f0', borderRadius: '10px', padding: '10px', textAlign: 'center' }}>
                    <div style={{ fontSize: '12px', fontWeight: 700, color: '#475569', marginBottom: '6px' }}>
                      {isAr ? 'ظهر البطاقة الخلفي' : 'ID Back'}
                    </div>
                    {tenant.idCardPhotoBackPath ? (
                      <img
                        src={tenant.idCardPhotoBackPath}
                        alt="ID Back"
                        style={{ width: '100%', height: '110px', objectFit: 'contain', borderRadius: '6px', backgroundColor: '#f1f5f9' }}
                      />
                    ) : (
                      <div style={{ height: '90px', backgroundColor: '#f8fafc', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#94a3b8', fontSize: '11px', borderRadius: '6px' }}>
                        {isAr ? 'لا توجد صورة' : 'No photo'}
                      </div>
                    )}
                  </div>
                </div>
              )}
            </div>
          )}
        </div>

        {/* 3. CONTRACT HISTORY */}
        <div style={{ marginBottom: '20px' }}>
          <div style={{ fontSize: '14px', fontWeight: 800, color: '#1e293b', marginBottom: '10px' }}>
            {isAr ? 'سجل العقود (العقود الحالية والقديمة):' : 'Contract History:'}
          </div>

          {tenantContracts.length === 0 ? (
            <div style={{ padding: '12px', backgroundColor: '#f8fafc', borderRadius: '10px', textAlign: 'center', fontSize: '12px', color: '#64748b' }}>
              {isAr ? 'لا توجد عقود مسجلة لهذا المستأجر' : 'No contracts recorded'}
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
              {tenantContracts.map((cnt) => {
                const u = units.find((x) => x.id === cnt.unitId);
                const p = u ? properties.find((x) => x.id === u.propertyId) : undefined;
                return (
                  <div
                    key={cnt.id}
                    onClick={() => onOpenContract && onOpenContract(cnt)}
                    style={{
                      padding: '12px',
                      borderRadius: '10px',
                      border: '1px solid #e2e8f0',
                      backgroundColor: cnt.id === activeContract?.id ? '#f0fdf4' : '#ffffff',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      cursor: onOpenContract ? 'pointer' : 'default',
                    }}
                  >
                    <div>
                      <div style={{ fontSize: '13px', fontWeight: 800, color: '#1e293b' }}>
                        {p?.name} - {isAr ? `وحدة ${u?.unitNumber}` : `Unit ${u?.unitNumber}`}
                      </div>
                      <div style={{ fontSize: '11px', color: '#64748b', marginTop: '2px' }}>
                        {formatUIDate(cnt.startDate)} → {formatUIDate(cnt.endDate)} ({formatCurrency(cnt.monthlyRent, language)})
                      </div>
                    </div>
                    <span
                      style={{
                        fontSize: '11px',
                        padding: '3px 8px',
                        borderRadius: '6px',
                        fontWeight: 700,
                        backgroundColor: cnt.isHistory || cnt.status === 'expired' ? '#f1f5f9' : '#dcfce7',
                        color: cnt.isHistory || cnt.status === 'expired' ? '#64748b' : '#166534',
                      }}
                    >
                      {cnt.isHistory || cnt.status === 'expired' ? (isAr ? 'منتهي' : 'Expired') : (isAr ? 'ساري' : 'Active')}
                    </span>
                  </div>
                );
              })}
            </div>
          )}
        </div>

        {/* 4. PAYMENT HISTORY */}
        <div style={{ marginBottom: '24px' }}>
          <div style={{ fontSize: '14px', fontWeight: 800, color: '#1e293b', marginBottom: '10px' }}>
            {isAr ? 'سجل الدفع والتحصيل:' : 'Payment History:'}
          </div>

          {tenantPayments.length === 0 ? (
            <div style={{ padding: '12px', backgroundColor: '#f8fafc', borderRadius: '10px', textAlign: 'center', fontSize: '12px', color: '#64748b' }}>
              {isAr ? 'لا توجد دفعات مسجلة' : 'No payments recorded'}
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '6px', maxHeight: '180px', overflowY: 'auto' }}>
              {tenantPayments.slice(0, 15).map((pay) => (
                <div
                  key={pay.id}
                  style={{
                    padding: '8px 12px',
                    borderRadius: '8px',
                    backgroundColor: pay.status === 'paid' ? '#f0fdf4' : pay.status === 'overdue' ? '#fef2f2' : '#f8fafc',
                    border: '1px solid #e2e8f0',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    fontSize: '12px',
                  }}
                >
                  <div>
                    <div style={{ fontWeight: 700, color: '#1e293b' }}>
                      {formatUIDate(pay.dueDate)} - {formatCurrency(pay.paidAmount || pay.amount, language)}
                    </div>
                    {pay.note && <div style={{ fontSize: '10px', color: '#64748b' }}>{pay.note}</div>}
                  </div>
                  <span
                    style={{
                      padding: '2px 8px',
                      borderRadius: '4px',
                      fontSize: '10px',
                      fontWeight: 800,
                      backgroundColor: pay.status === 'paid' ? '#bbf7d0' : pay.status === 'overdue' ? '#fecaca' : '#fed7aa',
                      color: pay.status === 'paid' ? '#166534' : pay.status === 'overdue' ? '#991b1b' : '#9a3412',
                    }}
                  >
                    {pay.status === 'paid' ? (isAr ? 'مسدد' : 'Paid') : pay.status === 'overdue' ? (isAr ? 'متأخر' : 'Overdue') : (isAr ? 'مستحق' : 'Pending')}
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* 5. ACTION BUTTONS */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
            <button
              onClick={() => onEdit(tenant)}
              style={{
                padding: '12px',
                borderRadius: '12px',
                border: '1.5px solid #1b4332',
                backgroundColor: '#ffffff',
                color: '#1b4332',
                fontSize: '13px',
                fontWeight: 800,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '6px',
                cursor: 'pointer',
              }}
            >
              <Edit size={16} />
              <span>{isAr ? 'تعديل الملف' : 'Edit Profile'}</span>
            </button>

            {activeContract && (
              <button
                onClick={() => onOpenContract && onOpenContract(activeContract)}
                style={{
                  padding: '12px',
                  borderRadius: '12px',
                  border: 'none',
                  backgroundColor: '#0284c7',
                  color: '#ffffff',
                  fontSize: '13px',
                  fontWeight: 800,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: '6px',
                  cursor: 'pointer',
                }}
              >
                <FileText size={16} />
                <span>{isAr ? 'فتح العقد' : 'Open Contract'}</span>
              </button>
            )}
          </div>

          {activeContract && (
            <button
              onClick={handlePrintContract}
              disabled={isPrintingContract}
              style={{
                padding: '12px',
                borderRadius: '12px',
                border: 'none',
                backgroundColor: '#1b4332',
                color: '#ffffff',
                fontSize: '13px',
                fontWeight: 800,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '6px',
                cursor: isPrintingContract ? 'wait' : 'pointer',
              }}
            >
              <Printer size={16} />
              <span>
                {isPrintingContract
                  ? (isAr ? 'جاري تجهيز العقد...' : 'Generating Contract...')
                  : (isAr ? 'طباعة العقد (PDF)' : 'Print Contract (PDF)')}
              </span>
            </button>
          )}

          {/* Delete Tenant with Confirmation */}
          {!showDeleteConfirm ? (
            <button
              onClick={() => setShowDeleteConfirm(true)}
              style={{
                padding: '10px',
                borderRadius: '10px',
                border: '1px solid #fee2e2',
                backgroundColor: '#fef2f2',
                color: '#dc2626',
                fontSize: '12px',
                fontWeight: 700,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '6px',
                cursor: 'pointer',
              }}
            >
              <Trash2 size={15} />
              <span>{isAr ? 'حذف المستأجر' : 'Delete Tenant'}</span>
            </button>
          ) : (
            <div
              style={{
                padding: '12px',
                borderRadius: '12px',
                backgroundColor: '#fef2f2',
                border: '1.5px solid #fca5a5',
                display: 'flex',
                flexDirection: 'column',
                gap: '8px',
              }}
            >
              <div style={{ fontSize: '12px', fontWeight: 800, color: '#dc2626', textAlign: 'center' }}>
                {isAr ? 'هل أنت متأكد من حذف ملف هذا المستأجر؟' : 'Are you sure you want to delete this tenant?'}
              </div>
              <div style={{ display: 'flex', gap: '8px' }}>
                <button
                  onClick={handleDeleteTenant}
                  style={{
                    flex: 1,
                    padding: '8px',
                    borderRadius: '8px',
                    backgroundColor: '#dc2626',
                    color: '#ffffff',
                    border: 'none',
                    fontSize: '12px',
                    fontWeight: 800,
                    cursor: 'pointer',
                  }}
                >
                  {isAr ? 'نعم، تأكيد الحذف' : 'Yes, Delete'}
                </button>
                <button
                  onClick={() => setShowDeleteConfirm(false)}
                  style={{
                    flex: 1,
                    padding: '8px',
                    borderRadius: '8px',
                    backgroundColor: '#e2e8f0',
                    color: '#475569',
                    border: 'none',
                    fontSize: '12px',
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
