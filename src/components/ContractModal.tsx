import React, { useState, useEffect } from 'react';
import { useApp } from '../context/AppContext';
import { Contract, Unit, Property } from '../types';
import { toISODate, formatUIDate, calculateDurationText, parseDate } from '../utils/date';
import { formatCurrency, toArabicIndicDigits } from '../utils/number';
import { generateAndShareContractPDF } from '../services/pdfGenerator';
import { TenantModal } from './TenantModal';
import {
  FileText,
  Share2,
  Calendar,
  DollarSign,
  User,
  CheckCircle2,
  AlertCircle,
  X,
  Printer,
  RefreshCw,
  Trash2,
  ChevronRight,
  ChevronLeft,
  Plus,
} from 'lucide-react';

interface ContractModalProps {
  unitId?: string;
  contract?: Contract;
  onClose: () => void;
}

export const ContractModal: React.FC<ContractModalProps> = ({
  unitId: initialUnitId,
  contract: existingContract,
  onClose,
}) => {
  const {
    properties,
    units,
    contracts,
    tenants,
    settings,
    language,
    createContract,
    updateContract,
    renewContract,
    endContract,
    deleteContract,
  } = useApp();

  const isAr = language === 'ar';
  const ChevronBack = isAr ? ChevronRight : ChevronLeft;

  // Selected Unit & Property
  const [selectedUnitId, setSelectedUnitId] = useState<string>(
    initialUnitId || existingContract?.unitId || units[0]?.id || ''
  );

  const currentUnit = units.find((u) => u.id === selectedUnitId);
  const currentProperty = currentUnit ? properties.find((p) => p.id === currentUnit.propertyId) : undefined;
  const isShop = currentProperty?.type === 'shops';

  // Step 1: Form, Step 2: Review & Print PDF
  const [currentStep, setCurrentStep] = useState<'form' | 'review'>('form');
  const [isRenewing, setIsRenewing] = useState(false);
  const [isGeneratingPDF, setIsGeneratingPDF] = useState(false);

  // Tenant selection & modal
  const [selectedTenantId, setSelectedTenantId] = useState<string | undefined>(existingContract?.tenantId);
  const [showAddTenantModal, setShowAddTenantModal] = useState(false);

  // Form Fields
  const [tenantName, setTenantName] = useState('');
  const [nationalId, setNationalId] = useState('');
  const [startDate, setStartDate] = useState(toISODate(new Date()));
  const [endDate, setEndDate] = useState('');
  const [monthlyRent, setMonthlyRent] = useState<number>(3000);
  const [rentDueDay, setRentDueDay] = useState<number>(1);
  const [notes, setNotes] = useState('');

  // Extra fields with defaults
  const [contractDate, setContractDate] = useState(toISODate(new Date()));
  const [tenantJob, setTenantJob] = useState('موظف');
  const [tenantAddress, setTenantAddress] = useState('الفيوم');
  const [tenantNationality, setTenantNationality] = useState('مصري');
  const [idIssuedFrom, setIdIssuedFrom] = useState('الفيوم');
  const [usagePurpose, setUsagePurpose] = useState(isShop ? 'محل تجاري' : 'سكن عائلي');
  const [paymentTerms, setPaymentTerms] = useState('ويسدد الايجار في أول كل شهر بموجب ايصالات بتوقيع المالك او الوكيل');
  const [renewalTerms, setRenewalTerms] = useState('موافقه كتابية صريحة من المالك');
  const [water, setWater] = useState('على المستأجر');
  const [electricity, setElectricity] = useState('على المستأجر');
  const [stairsLight, setStairsLight] = useState('على المستأجر');
  const [doormanFee, setDoormanFee] = useState('لا يوجد');
  const [depositAmount, setDepositAmount] = useState<number>(3000);
  const [extraClause, setExtraClause] = useState('');

  // Populate data when existingContract changes
  useEffect(() => {
    if (existingContract) {
      setTenantName(existingContract.tenantName);
      setNationalId(existingContract.nationalId || '');
      setStartDate(existingContract.startDate);
      setEndDate(existingContract.endDate);
      setMonthlyRent(existingContract.monthlyRent);
      setRentDueDay(existingContract.rentDueDay);
      setNotes(existingContract.notes || '');

      setContractDate(existingContract.contractDate || existingContract.startDate);
      setTenantJob(existingContract.tenantJob || 'موظف');
      setTenantAddress(existingContract.tenantAddress || 'الفيوم');
      setTenantNationality(existingContract.tenantNationality || 'مصري');
      setIdIssuedFrom(existingContract.idIssuedFrom || 'الفيوم');
      setUsagePurpose(existingContract.usagePurpose || (isShop ? 'محل تجاري' : 'سكن عائلي'));
      setPaymentTerms(existingContract.paymentTerms);
      setRenewalTerms(existingContract.renewalTerms);
      setWater(existingContract.water);
      setElectricity(existingContract.electricity);
      setStairsLight(existingContract.stairsLight);
      setDoormanFee(existingContract.doormanFee);
      setDepositAmount(existingContract.depositAmount || existingContract.monthlyRent);
      setExtraClause(existingContract.extraClause || '');
    } else {
      // Default end date 1 year from start date
      const start = parseDate(startDate);
      const defaultEnd = new Date(start.getFullYear() + 1, start.getMonth(), start.getDate() - 1);
      setEndDate(toISODate(defaultEnd));
      setRentDueDay(start.getDate());
    }
  }, [existingContract]);

  // Adjust usage purpose if unit type changes
  useEffect(() => {
    if (!existingContract) {
      setUsagePurpose(isShop ? 'محل تجاري' : 'سكن عائلي');
    }
  }, [isShop, existingContract]);

  // Auto calculate duration text
  const durationText = calculateDurationText(startDate, endDate, language);

  const handleSaveAndReview = () => {
    if (!tenantName.trim()) {
      alert(isAr ? 'الرجاء إدخال اسم المستأجر' : 'Please enter tenant name');
      return;
    }
    setCurrentStep('review');
  };

  const handleCommitContract = async () => {
    const payload = {
      unitId: selectedUnitId,
      tenantId: selectedTenantId || undefined,
      tenantName: tenantName.trim(),
      nationalId: nationalId.trim(),
      startDate,
      endDate,
      monthlyRent: Number(monthlyRent),
      rentDueDay: Number(rentDueDay),
      notes: notes.trim() || undefined,
      contractDate,
      tenantJob: tenantJob.trim(),
      tenantAddress: tenantAddress.trim(),
      tenantNationality: tenantNationality.trim(),
      idIssuedFrom: idIssuedFrom.trim(),
      usagePurpose: usagePurpose.trim(),
      paymentTerms: paymentTerms.trim(),
      renewalTerms: renewalTerms.trim(),
      water: water.trim(),
      electricity: electricity.trim(),
      stairsLight: stairsLight.trim(),
      doormanFee: doormanFee.trim(),
      depositAmount: Number(depositAmount),
      extraClause: extraClause.trim(),
    };

    if (existingContract) {
      await updateContract({
        ...existingContract,
        ...payload,
      });
    } else {
      await createContract(payload);
    }
    onClose();
  };

  const handleGeneratePDF = async () => {
    if (!currentUnit || !currentProperty) return;
    setIsGeneratingPDF(true);

    const activeTemplate = isShop ? settings.templates.shops : settings.templates.apartments;

    const dummyContract: Contract = {
      id: existingContract ? existingContract.id : 'preview-cnt',
      unitId: selectedUnitId,
      tenantId: selectedTenantId || undefined,
      tenantName: tenantName.trim(),
      nationalId: nationalId.trim(),
      startDate,
      endDate,
      monthlyRent: Number(monthlyRent),
      rentDueDay: Number(rentDueDay),
      notes: notes.trim() || undefined,
      status: 'active',
      contractDate,
      tenantJob: tenantJob.trim(),
      tenantAddress: tenantAddress.trim(),
      tenantNationality: tenantNationality.trim(),
      idIssuedFrom: idIssuedFrom.trim(),
      usagePurpose: usagePurpose.trim(),
      paymentTerms: paymentTerms.trim(),
      renewalTerms: renewalTerms.trim(),
      water: water.trim(),
      electricity: electricity.trim(),
      stairsLight: stairsLight.trim(),
      doormanFee: doormanFee.trim(),
      depositAmount: Number(depositAmount),
      extraClause: extraClause.trim(),
    };

    const linkedTenant = selectedTenantId ? tenants.find((t) => t.id === selectedTenantId) : undefined;

    try {
      await generateAndShareContractPDF({
        contract: dummyContract,
        unit: currentUnit,
        property: currentProperty,
        landlord: settings.landlord,
        templateText: activeTemplate,
        tenant: linkedTenant,
      });
    } catch (e) {
      console.error(e);
    } finally {
      setIsGeneratingPDF(false);
    }
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
          maxHeight: '92vh',
          display: 'flex',
          flexDirection: 'column',
          boxShadow: '0 -4px 20px rgba(0,0,0,0.15)',
          paddingBottom: 'calc(var(--safe-bottom) + 16px)',
        }}
      >
        {/* Modal Header */}
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
            {currentStep === 'review' && (
              <button
                onClick={() => setCurrentStep('form')}
                style={{
                  background: 'none',
                  border: 'none',
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  padding: '4px',
                }}
              >
                <ChevronBack size={22} color="#1b4332" />
              </button>
            )}
            <div>
              <h3 style={{ fontSize: '18px', fontWeight: 800, margin: 0, color: '#1e293b' }}>
                {currentStep === 'form'
                  ? existingContract
                    ? isAr
                      ? 'تعديل عقد الإيجار'
                      : 'Edit Lease Contract'
                    : isAr
                    ? 'تحرير عقد إيجار جديد'
                    : 'Create New Lease'
                  : isAr
                  ? 'مراجعة بيانات العقد والطباعة'
                  : 'Review Contract & Print'}
              </h3>
              <p style={{ fontSize: '12px', color: '#64748b', margin: 0 }}>
                {currentProperty?.name} • {isAr ? `وحدة ${currentUnit?.unitNumber}` : `Unit ${currentUnit?.unitNumber}`}
              </p>
            </div>
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

        {/* Modal Content Scroll Area */}
        <div style={{ padding: '20px', overflowY: 'auto', flex: 1 }}>
          {currentStep === 'form' ? (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              {/* Unit Selection if new */}
              {!existingContract && (
                <div>
                  <label style={{ display: 'block', fontSize: '12px', fontWeight: 700, color: '#475569', marginBottom: '4px' }}>
                    {isAr ? 'اختر الوحدة' : 'Select Unit'}
                  </label>
                  <select
                    value={selectedUnitId}
                    onChange={(e) => setSelectedUnitId(e.target.value)}
                    style={{
                      width: '100%',
                      height: '44px',
                      padding: '0 12px',
                      borderRadius: '8px',
                      border: '1px solid #cbd5e1',
                      fontSize: '14px',
                      backgroundColor: '#ffffff',
                    }}
                  >
                    {units.map((u) => {
                      const prop = properties.find((p) => p.id === u.propertyId);
                      const hasContract = contracts.some((c) => !c.isHistory && c.unitId === u.id);
                      return (
                        <option key={u.id} value={u.id}>
                          {prop?.name} - {isAr ? `وحدة ${u.unitNumber}` : `Unit ${u.unitNumber}`} {hasContract ? (isAr ? '(مؤجرة)' : '(Leased)') : (isAr ? '(شاغرة)' : '(Vacant)')}
                        </option>
                      );
                    })}
                  </select>
                </div>
              )}

              {/* Primary Contract Fields */}
              <div style={{ backgroundColor: '#f8fafc', padding: '14px', borderRadius: '12px', border: '1px solid #e2e8f0' }}>
                <h4 style={{ fontSize: '14px', fontWeight: 800, color: '#1b4332', marginBottom: '12px' }}>
                  {isAr ? 'البيانات الأساسية للمستأجر والتعاقد' : 'Tenant & Lease Essentials'}
                </h4>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                  {/* Select Existing Tenant or Add New Tenant */}
                  <div style={{ backgroundColor: '#ffffff', padding: '10px 12px', borderRadius: '10px', border: '1.5px solid #1b4332' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '6px' }}>
                      <label style={{ fontSize: '12px', fontWeight: 800, color: '#1b4332' }}>
                        {isAr ? '👤 اختيار مستأجر مسجل' : '👤 Select Registered Tenant'}
                      </label>
                      <button
                        type="button"
                        onClick={() => setShowAddTenantModal(true)}
                        style={{
                          background: 'none',
                          border: 'none',
                          color: '#0284c7',
                          fontSize: '11px',
                          fontWeight: 800,
                          display: 'flex',
                          alignItems: 'center',
                          gap: '3px',
                          cursor: 'pointer',
                        }}
                      >
                        <Plus size={13} />
                        <span>{isAr ? '+ إضافة مستأجر جديد' : '+ Add New Tenant'}</span>
                      </button>
                    </div>
                    <select
                      value={selectedTenantId || ''}
                      onChange={(e) => {
                        const tId = e.target.value;
                        setSelectedTenantId(tId || undefined);
                        const chosen = tenants.find((t) => t.id === tId);
                        if (chosen) {
                          setTenantName(chosen.fullName);
                          setNationalId(chosen.nationalId || '');
                          setTenantJob(chosen.job || 'موظف');
                          setTenantAddress(chosen.address || 'الفيوم');
                          setTenantNationality(chosen.nationality || 'مصري');
                        }
                      }}
                      style={{
                        width: '100%',
                        height: '40px',
                        padding: '0 10px',
                        borderRadius: '8px',
                        border: '1px solid #cbd5e1',
                        fontSize: '13px',
                        fontWeight: 600,
                        backgroundColor: '#f8fafc',
                      }}
                    >
                      <option value="">
                        {isAr ? '-- اختر مستأجر من القائمة (أو اكتب بالأسفل) --' : '-- Choose tenant (or type below) --'}
                      </option>
                      {tenants.map((t) => (
                        <option key={t.id} value={t.id}>
                          {t.fullName} {t.phone ? `(${t.phone})` : ''}
                        </option>
                      ))}
                    </select>
                  </div>

                  <div>
                    <label style={{ display: 'block', fontSize: '12px', fontWeight: 700, color: '#475569', marginBottom: '4px' }}>
                      {isAr ? 'اسم المستأجر بالكامل' : 'Tenant Full Name'} *
                    </label>
                    <input
                      type="text"
                      value={tenantName}
                      onChange={(e) => setTenantName(e.target.value)}
                      placeholder={isAr ? 'الاسم الثلاثي أو الرباعي' : 'Tenant full name'}
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
                    <label style={{ display: 'block', fontSize: '12px', fontWeight: 700, color: '#475569', marginBottom: '4px' }}>
                      {isAr ? 'الرقم القومي (١٤ رقم)' : 'National ID (14 digits)'}
                    </label>
                    <input
                      type="text"
                      maxLength={14}
                      value={nationalId}
                      onChange={(e) => setNationalId(e.target.value.replace(/\D/g, ''))}
                      placeholder="29000000000000"
                      style={{
                        width: '100%',
                        height: '44px',
                        padding: '0 12px',
                        borderRadius: '8px',
                        border: '1px solid #cbd5e1',
                        fontSize: '14px',
                        boxSizing: 'border-box',
                        letterSpacing: '1px',
                      }}
                    />
                  </div>

                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
                    <div>
                      <label style={{ display: 'block', fontSize: '12px', fontWeight: 700, color: '#475569', marginBottom: '4px' }}>
                        {isAr ? 'تاريخ بداية العقد' : 'Start Date'}
                      </label>
                      <input
                        type="date"
                        value={startDate}
                        onChange={(e) => {
                          setStartDate(e.target.value);
                          // Auto set rent due day to start day
                          const d = parseDate(e.target.value);
                          setRentDueDay(d.getDate());
                        }}
                        style={{
                          width: '100%',
                          height: '44px',
                          padding: '0 8px',
                          borderRadius: '8px',
                          border: '1px solid #cbd5e1',
                          fontSize: '13px',
                          boxSizing: 'border-box',
                        }}
                      />
                    </div>
                    <div>
                      <label style={{ display: 'block', fontSize: '12px', fontWeight: 700, color: '#475569', marginBottom: '4px' }}>
                        {isAr ? 'تاريخ نهاية العقد' : 'End Date'}
                      </label>
                      <input
                        type="date"
                        value={endDate}
                        onChange={(e) => setEndDate(e.target.value)}
                        style={{
                          width: '100%',
                          height: '44px',
                          padding: '0 8px',
                          borderRadius: '8px',
                          border: '1px solid #cbd5e1',
                          fontSize: '13px',
                          boxSizing: 'border-box',
                        }}
                      />
                    </div>
                  </div>

                  <div style={{ fontSize: '12px', color: '#0369a1', fontWeight: 700 }}>
                    {isAr ? `مدة التعاقد المحسوبة: ${durationText}` : `Calculated duration: ${durationText}`}
                  </div>

                  <div style={{ display: 'grid', gridTemplateColumns: '1.2fr 1fr', gap: '10px' }}>
                    <div>
                      <label style={{ display: 'block', fontSize: '12px', fontWeight: 700, color: '#475569', marginBottom: '4px' }}>
                        {isAr ? 'قيمة الإيجار الشهري' : 'Monthly Rent'}
                      </label>
                      <input
                        type="number"
                        value={monthlyRent}
                        onChange={(e) => setMonthlyRent(Number(e.target.value))}
                        style={{
                          width: '100%',
                          height: '44px',
                          padding: '0 12px',
                          borderRadius: '8px',
                          border: '1px solid #cbd5e1',
                          fontSize: '15px',
                          fontWeight: 700,
                          color: '#1b4332',
                          boxSizing: 'border-box',
                        }}
                      />
                    </div>
                    <div>
                      <label style={{ display: 'block', fontSize: '12px', fontWeight: 700, color: '#475569', marginBottom: '4px' }}>
                        {isAr ? 'يوم الاستحقاق الشهري' : 'Rent Due Day'}
                      </label>
                      <input
                        type="number"
                        min={1}
                        max={31}
                        value={rentDueDay}
                        onChange={(e) => setRentDueDay(Number(e.target.value))}
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
                  </div>

                  <div>
                    <label style={{ display: 'block', fontSize: '12px', fontWeight: 700, color: '#475569', marginBottom: '4px' }}>
                      {isAr ? 'مبلغ التأمين (ج.م)' : 'Deposit Amount'}
                    </label>
                    <input
                      type="number"
                      value={depositAmount}
                      onChange={(e) => setDepositAmount(Number(e.target.value))}
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
                </div>
              </div>

              {/* Extra Legal & Contract Fields (All editable with default values) */}
              <div style={{ backgroundColor: '#ffffff', padding: '14px', borderRadius: '12px', border: '1px solid #cbd5e1' }}>
                <h4 style={{ fontSize: '14px', fontWeight: 800, color: '#334155', marginBottom: '12px' }}>
                  {isAr ? 'بيانات وبنود العقد الإضافية (معتمدة في PDF)' : 'Contract Extra Fields (Used in PDF)'}
                </h4>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
                    <div>
                      <label style={{ display: 'block', fontSize: '11px', fontWeight: 700, color: '#64748b', marginBottom: '2px' }}>
                        {isAr ? 'تاريخ تحرير العقد' : 'Contract Date'}
                      </label>
                      <input
                        type="date"
                        value={contractDate}
                        onChange={(e) => setContractDate(e.target.value)}
                        style={{ width: '100%', height: '40px', padding: '0 8px', borderRadius: '6px', border: '1px solid #cbd5e1', fontSize: '12px', boxSizing: 'border-box' }}
                      />
                    </div>
                    <div>
                      <label style={{ display: 'block', fontSize: '11px', fontWeight: 700, color: '#64748b', marginBottom: '2px' }}>
                        {isAr ? 'مهنة المستأجر' : 'Tenant Job'}
                      </label>
                      <input
                        type="text"
                        value={tenantJob}
                        onChange={(e) => setTenantJob(e.target.value)}
                        style={{ width: '100%', height: '40px', padding: '0 8px', borderRadius: '6px', border: '1px solid #cbd5e1', fontSize: '12px', boxSizing: 'border-box' }}
                      />
                    </div>
                  </div>

                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
                    <div>
                      <label style={{ display: 'block', fontSize: '11px', fontWeight: 700, color: '#64748b', marginBottom: '2px' }}>
                        {isAr ? 'عنوان المستأجر' : 'Tenant Address'}
                      </label>
                      <input
                        type="text"
                        value={tenantAddress}
                        onChange={(e) => setTenantAddress(e.target.value)}
                        style={{ width: '100%', height: '40px', padding: '0 8px', borderRadius: '6px', border: '1px solid #cbd5e1', fontSize: '12px', boxSizing: 'border-box' }}
                      />
                    </div>
                    <div>
                      <label style={{ display: 'block', fontSize: '11px', fontWeight: 700, color: '#64748b', marginBottom: '2px' }}>
                        {isAr ? 'صادر من' : 'ID Issued From'}
                      </label>
                      <input
                        type="text"
                        value={idIssuedFrom}
                        onChange={(e) => setIdIssuedFrom(e.target.value)}
                        style={{ width: '100%', height: '40px', padding: '0 8px', borderRadius: '6px', border: '1px solid #cbd5e1', fontSize: '12px', boxSizing: 'border-box' }}
                      />
                    </div>
                  </div>

                  <div>
                    <label style={{ display: 'block', fontSize: '11px', fontWeight: 700, color: '#64748b', marginBottom: '2px' }}>
                      {isAr ? 'الغرض المستعمل فيه المكان' : 'Usage Purpose'}
                    </label>
                    <input
                      type="text"
                      value={usagePurpose}
                      onChange={(e) => setUsagePurpose(e.target.value)}
                      style={{ width: '100%', height: '40px', padding: '0 8px', borderRadius: '6px', border: '1px solid #cbd5e1', fontSize: '12px', boxSizing: 'border-box' }}
                    />
                  </div>

                  <div>
                    <label style={{ display: 'block', fontSize: '11px', fontWeight: 700, color: '#64748b', marginBottom: '2px' }}>
                      {isAr ? 'طريقة وشروط سداد الإيجار' : 'Payment Terms'}
                    </label>
                    <textarea
                      value={paymentTerms}
                      onChange={(e) => setPaymentTerms(e.target.value)}
                      rows={2}
                      style={{ width: '100%', padding: '6px 8px', borderRadius: '6px', border: '1px solid #cbd5e1', fontSize: '12px', boxSizing: 'border-box', fontFamily: 'inherit' }}
                    />
                  </div>

                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px' }}>
                    <div>
                      <label style={{ display: 'block', fontSize: '11px', fontWeight: 700, color: '#64748b', marginBottom: '2px' }}>
                        {isAr ? 'استهلاك المياه' : 'Water'}
                      </label>
                      <input
                        type="text"
                        value={water}
                        onChange={(e) => setWater(e.target.value)}
                        style={{ width: '100%', height: '36px', padding: '0 8px', borderRadius: '6px', border: '1px solid #cbd5e1', fontSize: '12px', boxSizing: 'border-box' }}
                      />
                    </div>
                    <div>
                      <label style={{ display: 'block', fontSize: '11px', fontWeight: 700, color: '#64748b', marginBottom: '2px' }}>
                        {isAr ? 'استهلاك الكهرباء' : 'Electricity'}
                      </label>
                      <input
                        type="text"
                        value={electricity}
                        onChange={(e) => setElectricity(e.target.value)}
                        style={{ width: '100%', height: '36px', padding: '0 8px', borderRadius: '6px', border: '1px solid #cbd5e1', fontSize: '12px', boxSizing: 'border-box' }}
                      />
                    </div>
                  </div>

                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px' }}>
                    <div>
                      <label style={{ display: 'block', fontSize: '11px', fontWeight: 700, color: '#64748b', marginBottom: '2px' }}>
                        {isAr ? 'نور السلم' : 'Stairs Light'}
                      </label>
                      <input
                        type="text"
                        value={stairsLight}
                        onChange={(e) => setStairsLight(e.target.value)}
                        style={{ width: '100%', height: '36px', padding: '0 8px', borderRadius: '6px', border: '1px solid #cbd5e1', fontSize: '12px', boxSizing: 'border-box' }}
                      />
                    </div>
                    <div>
                      <label style={{ display: 'block', fontSize: '11px', fontWeight: 700, color: '#64748b', marginBottom: '2px' }}>
                        {isAr ? 'أجر البواب' : 'Doorman Fee'}
                      </label>
                      <input
                        type="text"
                        value={doormanFee}
                        onChange={(e) => setDoormanFee(e.target.value)}
                        style={{ width: '100%', height: '36px', padding: '0 8px', borderRadius: '6px', border: '1px solid #cbd5e1', fontSize: '12px', boxSizing: 'border-box' }}
                      />
                    </div>
                  </div>

                  <div>
                    <label style={{ display: 'block', fontSize: '11px', fontWeight: 700, color: '#64748b', marginBottom: '2px' }}>
                      {isAr ? 'البند الإضافي (اختياري)' : 'Extra Clause (Optional)'}
                    </label>
                    <textarea
                      value={extraClause}
                      onChange={(e) => setExtraClause(e.target.value)}
                      placeholder={isAr ? 'أي شروط خاصة إضافية...' : 'Any extra terms...'}
                      rows={2}
                      style={{ width: '100%', padding: '6px 8px', borderRadius: '6px', border: '1px solid #cbd5e1', fontSize: '12px', boxSizing: 'border-box', fontFamily: 'inherit' }}
                    />
                  </div>
                </div>
              </div>

              {/* Action Buttons */}
              <div style={{ display: 'flex', gap: '10px', marginTop: '10px' }}>
                <button
                  type="button"
                  onClick={onClose}
                  style={{
                    flex: 1,
                    padding: '12px',
                    borderRadius: '10px',
                    border: '1px solid #cbd5e1',
                    backgroundColor: '#ffffff',
                    color: '#475569',
                    fontWeight: 700,
                    cursor: 'pointer',
                  }}
                >
                  {isAr ? 'إلغاء' : 'Cancel'}
                </button>
                <button
                  type="button"
                  onClick={handleSaveAndReview}
                  style={{
                    flex: 2,
                    padding: '12px',
                    borderRadius: '10px',
                    border: 'none',
                    backgroundColor: '#1b4332',
                    color: '#ffffff',
                    fontWeight: 700,
                    fontSize: '14px',
                    cursor: 'pointer',
                  }}
                >
                  {isAr ? 'متابعة ومراجعة العقد' : 'Review Contract Details'}
                </button>
              </div>
            </div>
          ) : (
            /* STEP 2: Contract Details Step Before PDF */
            <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              <div
                style={{
                  backgroundColor: '#ecfdf5',
                  padding: '12px 16px',
                  borderRadius: '12px',
                  border: '1px solid #a7f3d0',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '10px',
                }}
              >
                <CheckCircle2 size={24} color="#059669" />
                <div style={{ fontSize: '13px', color: '#065f46' }}>
                  {isAr
                    ? 'راجع كافة حقول العقد أدناه للتأكد من صحتها قبل الحفظ وطباعة ملف PDF.'
                    : 'Review all contract details below before saving and generating the PDF.'}
                </div>
              </div>

              {/* Details Table */}
              <div style={{ border: '1px solid #cbd5e1', borderRadius: '12px', overflow: 'hidden', fontSize: '13px' }}>
                <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                  <tbody>
                    <tr style={{ background: '#f8fafc', borderBottom: '1px solid #e2e8f0' }}>
                      <td style={{ padding: '8px 12px', fontWeight: 700, width: '40%' }}>{isAr ? 'اسم المستأجر' : 'Tenant'}</td>
                      <td style={{ padding: '8px 12px', color: '#1b4332', fontWeight: 800 }}>{tenantName}</td>
                    </tr>
                    <tr style={{ borderBottom: '1px solid #e2e8f0' }}>
                      <td style={{ padding: '8px 12px', fontWeight: 700 }}>{isAr ? 'الرقم القومي' : 'National ID'}</td>
                      <td style={{ padding: '8px 12px' }}>{nationalId || (isAr ? 'غير مدخل' : 'Not set')}</td>
                    </tr>
                    <tr style={{ background: '#f8fafc', borderBottom: '1px solid #e2e8f0' }}>
                      <td style={{ padding: '8px 12px', fontWeight: 700 }}>{isAr ? 'الوحدة والعقار' : 'Unit & Property'}</td>
                      <td style={{ padding: '8px 12px' }}>{currentProperty?.name} (وحدة {currentUnit?.unitNumber})</td>
                    </tr>
                    <tr style={{ borderBottom: '1px solid #e2e8f0' }}>
                      <td style={{ padding: '8px 12px', fontWeight: 700 }}>{isAr ? 'مدة وسريان العقد' : 'Duration'}</td>
                      <td style={{ padding: '8px 12px', fontWeight: 700, color: '#0369a1' }}>
                        {durationText} ({formatUIDate(startDate)} إلى {formatUIDate(endDate)})
                      </td>
                    </tr>
                    <tr style={{ background: '#f8fafc', borderBottom: '1px solid #e2e8f0' }}>
                      <td style={{ padding: '8px 12px', fontWeight: 700 }}>{isAr ? 'الإيجار الشهري' : 'Monthly Rent'}</td>
                      <td style={{ padding: '8px 12px', fontWeight: 800, color: '#1b4332' }}>
                        {formatCurrency(monthlyRent, language)}
                      </td>
                    </tr>
                    <tr style={{ borderBottom: '1px solid #e2e8f0' }}>
                      <td style={{ padding: '8px 12px', fontWeight: 700 }}>{isAr ? 'يوم الاستحقاق الشهري' : 'Due Day'}</td>
                      <td style={{ padding: '8px 12px' }}>يوم {rentDueDay} من كل شهر</td>
                    </tr>
                    <tr style={{ background: '#f8fafc', borderBottom: '1px solid #e2e8f0' }}>
                      <td style={{ padding: '8px 12px', fontWeight: 700 }}>{isAr ? 'مبلغ التأمين' : 'Deposit'}</td>
                      <td style={{ padding: '8px 12px' }}>{formatCurrency(depositAmount, language)}</td>
                    </tr>
                    <tr style={{ borderBottom: '1px solid #e2e8f0' }}>
                      <td style={{ padding: '8px 12px', fontWeight: 700 }}>{isAr ? 'اسم وبيانات المالك' : 'Landlord'}</td>
                      <td style={{ padding: '8px 12px' }}>{settings.landlord.name} ({settings.landlord.job})</td>
                    </tr>
                    <tr style={{ background: '#f8fafc', borderBottom: '1px solid #e2e8f0' }}>
                      <td style={{ padding: '8px 12px', fontWeight: 700 }}>{isAr ? 'الغرض المستعمل' : 'Usage'}</td>
                      <td style={{ padding: '8px 12px' }}>{usagePurpose}</td>
                    </tr>
                    <tr>
                      <td style={{ padding: '8px 12px', fontWeight: 700 }}>{isAr ? 'البند الإضافي' : 'Extra Clause'}</td>
                      <td style={{ padding: '8px 12px' }}>{extraClause || (isAr ? 'لا يوجد' : 'None')}</td>
                    </tr>
                  </tbody>
                </table>
              </div>

              {/* Action Buttons in Review step */}
              <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                <button
                  type="button"
                  onClick={handleGeneratePDF}
                  disabled={isGeneratingPDF}
                  style={{
                    width: '100%',
                    padding: '14px',
                    borderRadius: '12px',
                    border: 'none',
                    backgroundColor: '#1b4332',
                    color: '#ffffff',
                    fontSize: '15px',
                    fontWeight: 800,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    gap: '8px',
                    cursor: isGeneratingPDF ? 'wait' : 'pointer',
                    boxShadow: '0 4px 12px rgba(27,67,50,0.3)',
                  }}
                >
                  <Share2 size={18} />
                  <span>
                    {isGeneratingPDF
                      ? isAr
                        ? 'جارِ تجهيز ملف العقد PDF...'
                        : 'Generating Contract PDF...'
                      : isAr
                      ? 'طباعة ومشاركة عقد الإيجار (PDF)'
                      : 'Generate & Share PDF Contract'}
                  </span>
                </button>

                <div style={{ display: 'flex', gap: '10px' }}>
                  <button
                    type="button"
                    onClick={() => setCurrentStep('form')}
                    style={{
                      flex: 1,
                      padding: '12px',
                      borderRadius: '10px',
                      border: '1px solid #cbd5e1',
                      backgroundColor: '#ffffff',
                      color: '#475569',
                      fontWeight: 700,
                      cursor: 'pointer',
                    }}
                  >
                    {isAr ? 'تعديل الحقول' : 'Edit Fields'}
                  </button>

                  <button
                    type="button"
                    onClick={handleCommitContract}
                    style={{
                      flex: 2,
                      padding: '12px',
                      borderRadius: '10px',
                      border: 'none',
                      backgroundColor: '#059669',
                      color: '#ffffff',
                      fontWeight: 700,
                      fontSize: '14px',
                      cursor: 'pointer',
                    }}
                  >
                    {isAr ? 'حفظ العقد في النظام' : 'Save Lease in System'}
                  </button>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>

      {showAddTenantModal && (
        <TenantModal
          onClose={() => setShowAddTenantModal(false)}
          onSuccess={(newTenant) => {
            setSelectedTenantId(newTenant.id);
            setTenantName(newTenant.fullName);
            setNationalId(newTenant.nationalId || '');
            setTenantJob(newTenant.job || 'موظف');
            setTenantAddress(newTenant.address || 'الفيوم');
            setTenantNationality(newTenant.nationality || 'مصري');
            setShowAddTenantModal(false);
          }}
        />
      )}
    </div>
  );
};
