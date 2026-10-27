import React from 'react';
import { useApp } from '../context/AppContext';
import { formatCurrency } from '../utils/number';
import { formatUIDate, getDaysFromToday } from '../utils/date';
import {
  Building,
  CheckCircle2,
  AlertTriangle,
  Clock,
  CalendarCheck,
  TrendingUp,
  FileText,
  DollarSign,
  ChevronLeft,
  ChevronRight,
  PlusCircle,
  FileSpreadsheet,
} from 'lucide-react';

interface DashboardScreenProps {
  onOpenContractModal: (unitId?: string) => void;
  onOpenPaymentModal: (paymentId: string) => void;
  onOpenUnitDetail: (unitId: string) => void;
}

export const DashboardScreen: React.FC<DashboardScreenProps> = ({
  onOpenContractModal,
  onOpenPaymentModal,
  onOpenUnitDetail,
}) => {
  const {
    language,
    stats,
    payments,
    contracts,
    properties,
    units,
    tenants,
    addTenant,
    setSelectedTenantId,
    setActiveTab,
    setSelectedPropertyId,
  } = useApp();

  const isAr = language === 'ar';
  const ChevronIcon = isAr ? ChevronLeft : ChevronRight;

  const propertyMap = new Map(properties.map((p) => [p.id, p]));
  const unitMap = new Map(units.map((u) => [u.id, u]));

  // 1. Rent due in the next 7 days
  const upcomingPayments = payments.filter((p) => {
    if (p.status !== 'pending') return false;
    const days = getDaysFromToday(p.dueDate);
    return days >= 0 && days <= 7;
  });

  // 2. Overdue payments
  const overduePayments = payments.filter((p) => p.status === 'overdue');

  // 3. Contracts ending in the next 60 days
  const expiringContracts = contracts.filter((c) => {
    if (c.isHistory || c.status === 'expired') return false;
    const days = getDaysFromToday(c.endDate);
    return days >= 0 && days <= 60;
  });

  return (
    <div style={{ padding: '16px', paddingBottom: '90px' }}>
      {/* Welcome Banner */}
      <div
        style={{
          backgroundColor: '#ffffff',
          borderRadius: '16px',
          padding: '16px',
          boxShadow: '0 2px 10px rgba(0,0,0,0.04)',
          border: '1px solid #e2e8f0',
          marginBottom: '20px',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '12px' }}>
          <div>
            <h2 style={{ fontSize: '18px', fontWeight: 800, color: '#1e293b' }}>
              {isAr ? 'ملخص المحفظة العقارية' : 'Property Portfolio Overview'}
            </h2>
            <p style={{ fontSize: '13px', color: '#64748b' }}>
              {isAr
                ? 'متابعة سريعة للإيجارات، العقود السارية والتحصيل'
                : 'Real-time rent tracking, active leases, and collections'}
            </p>
          </div>
          <button
            onClick={() => setActiveTab('reports')}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              backgroundColor: '#ecfdf5',
              color: '#065f46',
              border: '1px solid #a7f3d0',
              padding: '8px 12px',
              borderRadius: '10px',
              fontSize: '12px',
              fontWeight: 700,
              cursor: 'pointer',
            }}
          >
            <FileSpreadsheet size={16} />
            <span>{isAr ? 'التقارير' : 'Reports'}</span>
          </button>
        </div>

        {/* 5 Core Metric Cards */}
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(2, 1fr)',
            gap: '12px',
          }}
        >
          {/* Active Contracts */}
          <div
            onClick={() => setActiveTab('properties')}
            style={{
              backgroundColor: '#f0fdf4',
              borderRadius: '12px',
              padding: '14px',
              border: '1px solid #bbf7d0',
              cursor: 'pointer',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '6px' }}>
              <div
                style={{
                  width: '32px',
                  height: '32px',
                  borderRadius: '8px',
                  backgroundColor: '#dcfce7',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  color: '#16a34a',
                }}
              >
                <FileText size={18} />
              </div>
              <span style={{ fontSize: '12px', fontWeight: 600, color: '#166534' }}>
                {isAr ? 'العقود السارية' : 'Active Contracts'}
              </span>
            </div>
            <div style={{ fontSize: '24px', fontWeight: 800, color: '#14532d' }}>
              {stats.activeContractsCount}
            </div>
          </div>

          {/* Vacant Units */}
          <div
            onClick={() => setActiveTab('properties')}
            style={{
              backgroundColor: '#f8fafc',
              borderRadius: '12px',
              padding: '14px',
              border: '1px solid #e2e8f0',
              cursor: 'pointer',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '6px' }}>
              <div
                style={{
                  width: '32px',
                  height: '32px',
                  borderRadius: '8px',
                  backgroundColor: '#f1f5f9',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  color: '#475569',
                }}
              >
                <Building size={18} />
              </div>
              <span style={{ fontSize: '12px', fontWeight: 600, color: '#334155' }}>
                {isAr ? 'الوحدات الشاغرة' : 'Vacant Units'}
              </span>
            </div>
            <div style={{ fontSize: '24px', fontWeight: 800, color: '#1e293b' }}>
              {stats.vacantUnitsCount}
            </div>
          </div>

          {/* Expected Monthly Rent */}
          <div
            style={{
              backgroundColor: '#eff6ff',
              borderRadius: '12px',
              padding: '14px',
              border: '1px solid #bfdbfe',
              gridColumn: 'span 2',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '6px' }}>
                  <TrendingUp size={18} color="#2563eb" />
                  <span style={{ fontSize: '12px', fontWeight: 600, color: '#1e40af' }}>
                    {isAr ? 'إجمالي الإيجار الشهري المتوقع' : 'Expected Monthly Rent'}
                  </span>
                </div>
                <div style={{ fontSize: '24px', fontWeight: 800, color: '#1d4ed8' }}>
                  {formatCurrency(stats.expectedMonthlyRent, language)}
                </div>
              </div>
              <button
                onClick={() => onOpenContractModal()}
                style={{
                  backgroundColor: '#1b4332',
                  color: '#ffffff',
                  border: 'none',
                  borderRadius: '10px',
                  padding: '8px 12px',
                  fontSize: '12px',
                  fontWeight: 700,
                  display: 'flex',
                  alignItems: 'center',
                  gap: '6px',
                  cursor: 'pointer',
                }}
              >
                <PlusCircle size={16} />
                <span>{isAr ? 'عقد جديد' : 'New Contract'}</span>
              </button>
            </div>
          </div>

          {/* Collected This Month */}
          <div
            onClick={() => setActiveTab('payments')}
            style={{
              backgroundColor: '#ecfdf5',
              borderRadius: '12px',
              padding: '14px',
              border: '1px solid #a7f3d0',
              cursor: 'pointer',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '6px' }}>
              <CheckCircle2 size={18} color="#059669" />
              <span style={{ fontSize: '12px', fontWeight: 600, color: '#065f46' }}>
                {isAr ? 'المحصل هذا الشهر' : 'Collected (Month)'}
              </span>
            </div>
            <div style={{ fontSize: '18px', fontWeight: 800, color: '#047857' }}>
              {formatCurrency(stats.rentCollectedThisMonth, language)}
            </div>
          </div>

          {/* Overdue Total */}
          <div
            onClick={() => setActiveTab('payments')}
            style={{
              backgroundColor: '#fef2f2',
              borderRadius: '12px',
              padding: '14px',
              border: '1px solid #fecaca',
              cursor: 'pointer',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '6px' }}>
              <AlertTriangle size={18} color="#dc2626" />
              <span style={{ fontSize: '12px', fontWeight: 600, color: '#991b1b' }}>
                {isAr ? 'المتأخرات' : 'Total Overdue'}
              </span>
            </div>
            <div style={{ fontSize: '18px', fontWeight: 800, color: '#b91c1c' }}>
              {formatCurrency(stats.totalOverdueAmount, language)}
            </div>
          </div>
        </div>
      </div>

      {/* LIST 1: Rent Due in Next 7 Days */}
      <div style={{ marginBottom: '24px' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '10px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Clock size={20} color="#d97706" />
            <h3 style={{ fontSize: '16px', fontWeight: 800, color: '#1e293b' }}>
              {isAr ? 'إيجارات مستحقة خلال ٧ أيام' : 'Rent Due in Next 7 Days'} ({upcomingPayments.length})
            </h3>
          </div>
          <button
            onClick={() => setActiveTab('payments')}
            style={{ background: 'none', border: 'none', color: '#1b4332', fontSize: '13px', fontWeight: 700, cursor: 'pointer' }}
          >
            {isAr ? 'عرض الكل' : 'View All'}
          </button>
        </div>

        {upcomingPayments.length === 0 ? (
          <div
            style={{
              backgroundColor: '#ffffff',
              borderRadius: '12px',
              padding: '16px',
              textAlign: 'center',
              color: '#64748b',
              fontSize: '13px',
              border: '1px solid #e2e8f0',
            }}
          >
            {isAr ? 'لا توجد دفعات مستحقة خلال السبعة أيام القادمة.' : 'No payments due in the next 7 days.'}
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
            {upcomingPayments.map((p) => {
              const contract = contracts.find((c) => c.id === p.contractId);
              const unit = unitMap.get(p.unitId);
              const prop = unit ? propertyMap.get(unit.propertyId) : undefined;
              const daysLeft = getDaysFromToday(p.dueDate);
              const linkedTenant = contract
                ? (contract.tenantId
                    ? tenants.find((t) => t.id === contract.tenantId)
                    : tenants.find((t) => t.fullName.trim() === contract.tenantName.trim()))
                : undefined;

              return (
                <div
                  key={p.id}
                  onClick={() => onOpenPaymentModal(p.id)}
                  style={{
                    backgroundColor: '#ffffff',
                    borderRadius: '12px',
                    padding: '14px',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    boxShadow: '0 1px 4px rgba(0,0,0,0.05)',
                    border: '1px solid #fed7aa',
                    cursor: 'pointer',
                  }}
                >
                  <div style={{ flex: 1 }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px' }}>
                      <span
                        onClick={async (e) => {
                          e.stopPropagation();
                          if (linkedTenant) {
                            setSelectedTenantId(linkedTenant.id);
                          } else if (contract?.tenantName) {
                            const created = await addTenant({
                              fullName: contract.tenantName,
                              nationalId: contract.nationalId || '',
                              job: contract.tenantJob || 'موظف',
                              phone: '',
                              address: contract.tenantAddress || 'الفيوم',
                              nationality: contract.tenantNationality || 'مصري',
                            });
                            setSelectedTenantId(created.id);
                          }
                        }}
                        style={{
                          fontSize: '15px',
                          fontWeight: 800,
                          color: '#0f172a',
                          cursor: 'pointer',
                          textDecoration: 'underline',
                        }}
                        title={isAr ? 'عرض ملف المستأجر' : 'View Profile'}
                      >
                        {linkedTenant?.fullName || contract?.tenantName || (isAr ? 'مستأجر' : 'Tenant')}
                      </span>
                      <span
                        style={{
                          backgroundColor: '#ffedd5',
                          color: '#c2410c',
                          fontSize: '11px',
                          fontWeight: 700,
                          padding: '2px 8px',
                          borderRadius: '6px',
                        }}
                      >
                        {daysLeft === 0
                          ? isAr
                            ? 'اليوم'
                            : 'Today'
                          : isAr
                          ? `خلال ${daysLeft} أيام`
                          : `in ${daysLeft}d`}
                      </span>
                    </div>
                    <div style={{ fontSize: '12px', color: '#64748b' }}>
                      {prop?.name} • {isAr ? `وحدة ${unit?.unitNumber}` : `Unit ${unit?.unitNumber}`} • {formatUIDate(p.dueDate)}
                    </div>
                  </div>

                  <div style={{ textAlign: isAr ? 'left' : 'right', marginLeft: '12px', marginRight: '12px' }}>
                    <div style={{ fontSize: '16px', fontWeight: 800, color: '#1b4332' }}>
                      {formatCurrency(p.amount, language)}
                    </div>
                    <span
                      style={{
                        fontSize: '11px',
                        color: '#2563eb',
                        fontWeight: 700,
                      }}
                    >
                      {isAr ? 'سداد الآن' : 'Pay Now'}
                    </span>
                  </div>
                  <ChevronIcon size={18} color="#94a3b8" />
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* LIST 2: Overdue Rent */}
      <div style={{ marginBottom: '24px' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '10px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <AlertTriangle size={20} color="#dc2626" />
            <h3 style={{ fontSize: '16px', fontWeight: 800, color: '#1e293b' }}>
              {isAr ? 'إيجارات متأخرة' : 'Overdue Rent'} ({overduePayments.length})
            </h3>
          </div>
          <button
            onClick={() => setActiveTab('payments')}
            style={{ background: 'none', border: 'none', color: '#dc2626', fontSize: '13px', fontWeight: 700, cursor: 'pointer' }}
          >
            {isAr ? 'عرض الكل' : 'View All'}
          </button>
        </div>

        {overduePayments.length === 0 ? (
          <div
            style={{
              backgroundColor: '#ffffff',
              borderRadius: '12px',
              padding: '16px',
              textAlign: 'center',
              color: '#16a34a',
              fontSize: '13px',
              border: '1px solid #bbf7d0',
            }}
          >
            {isAr ? 'ممتاز! لا توجد إيجارات متأخرة حالياً.' : 'Great! No overdue rent payments.'}
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
            {overduePayments.map((p) => {
              const contract = contracts.find((c) => c.id === p.contractId);
              const unit = unitMap.get(p.unitId);
              const prop = unit ? propertyMap.get(unit.propertyId) : undefined;
              const daysLate = Math.abs(getDaysFromToday(p.dueDate));
              const linkedTenant = contract
                ? (contract.tenantId
                    ? tenants.find((t) => t.id === contract.tenantId)
                    : tenants.find((t) => t.fullName.trim() === contract.tenantName.trim()))
                : undefined;

              return (
                <div
                  key={p.id}
                  onClick={() => onOpenPaymentModal(p.id)}
                  style={{
                    backgroundColor: '#ffffff',
                    borderRadius: '12px',
                    padding: '14px',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    boxShadow: '0 1px 4px rgba(0,0,0,0.05)',
                    border: '1px solid #fecaca',
                    cursor: 'pointer',
                  }}
                >
                  <div style={{ flex: 1 }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px' }}>
                      <span
                        onClick={async (e) => {
                          e.stopPropagation();
                          if (linkedTenant) {
                            setSelectedTenantId(linkedTenant.id);
                          } else if (contract?.tenantName) {
                            const created = await addTenant({
                              fullName: contract.tenantName,
                              nationalId: contract.nationalId || '',
                              job: contract.tenantJob || 'موظف',
                              phone: '',
                              address: contract.tenantAddress || 'الفيوم',
                              nationality: contract.tenantNationality || 'مصري',
                            });
                            setSelectedTenantId(created.id);
                          }
                        }}
                        style={{
                          fontSize: '15px',
                          fontWeight: 800,
                          color: '#991b1b',
                          cursor: 'pointer',
                          textDecoration: 'underline',
                        }}
                        title={isAr ? 'عرض ملف المستأجر' : 'View Profile'}
                      >
                        {linkedTenant?.fullName || contract?.tenantName || (isAr ? 'مستأجر' : 'Tenant')}
                      </span>
                      <span
                        style={{
                          backgroundColor: '#fee2e2',
                          color: '#b91c1c',
                          fontSize: '11px',
                          fontWeight: 700,
                          padding: '2px 8px',
                          borderRadius: '6px',
                        }}
                      >
                        {isAr ? `متأخر ${daysLate} يوم` : `${daysLate}d late`}
                      </span>
                    </div>
                    <div style={{ fontSize: '12px', color: '#64748b' }}>
                      {prop?.name} • {isAr ? `وحدة ${unit?.unitNumber}` : `Unit ${unit?.unitNumber}`} • {formatUIDate(p.dueDate)}
                    </div>
                  </div>

                  <div style={{ textAlign: isAr ? 'left' : 'right', marginLeft: '12px', marginRight: '12px' }}>
                    <div style={{ fontSize: '16px', fontWeight: 800, color: '#dc2626' }}>
                      {formatCurrency(p.amount, language)}
                    </div>
                    <span style={{ fontSize: '11px', color: '#16a34a', fontWeight: 700 }}>
                      {isAr ? 'تسجيل السداد' : 'Mark Paid'}
                    </span>
                  </div>
                  <ChevronIcon size={18} color="#94a3b8" />
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* LIST 3: Contracts Ending in Next 60 Days */}
      <div>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '10px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <CalendarCheck size={20} color="#0284c7" />
            <h3 style={{ fontSize: '16px', fontWeight: 800, color: '#1e293b' }}>
              {isAr ? 'عقود تنتهي خلال ٦٠ يوماً' : 'Contracts Ending in 60 Days'} ({expiringContracts.length})
            </h3>
          </div>
          <button
            onClick={() => setActiveTab('properties')}
            style={{ background: 'none', border: 'none', color: '#0284c7', fontSize: '13px', fontWeight: 700, cursor: 'pointer' }}
          >
            {isAr ? 'عرض الكل' : 'View All'}
          </button>
        </div>

        {expiringContracts.length === 0 ? (
          <div
            style={{
              backgroundColor: '#ffffff',
              borderRadius: '12px',
              padding: '16px',
              textAlign: 'center',
              color: '#64748b',
              fontSize: '13px',
              border: '1px solid #e2e8f0',
            }}
          >
            {isAr ? 'لا توجد عقود تنتهي خلال الستين يوماً القادمة.' : 'No contracts ending in the next 60 days.'}
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
            {expiringContracts.map((c) => {
              const unit = unitMap.get(c.unitId);
              const prop = unit ? propertyMap.get(unit.propertyId) : undefined;
              const daysLeft = getDaysFromToday(c.endDate);
              const linkedTenant = c.tenantId
                ? tenants.find((t) => t.id === c.tenantId)
                : tenants.find((t) => t.fullName.trim() === c.tenantName.trim());

              return (
                <div
                  key={c.id}
                  onClick={() => unit && onOpenUnitDetail(unit.id)}
                  style={{
                    backgroundColor: '#ffffff',
                    borderRadius: '12px',
                    padding: '14px',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    boxShadow: '0 1px 4px rgba(0,0,0,0.05)',
                    border: '1px solid #bae6fd',
                    cursor: 'pointer',
                  }}
                >
                  <div style={{ flex: 1 }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px' }}>
                      <span
                        onClick={async (e) => {
                          e.stopPropagation();
                          if (linkedTenant) {
                            setSelectedTenantId(linkedTenant.id);
                          } else if (c.tenantName) {
                            const created = await addTenant({
                              fullName: c.tenantName,
                              nationalId: c.nationalId || '',
                              job: c.tenantJob || 'موظف',
                              phone: '',
                              address: c.tenantAddress || 'الفيوم',
                              nationality: c.tenantNationality || 'مصري',
                            });
                            setSelectedTenantId(created.id);
                          }
                        }}
                        style={{
                          fontSize: '15px',
                          fontWeight: 800,
                          color: '#0369a1',
                          cursor: 'pointer',
                          textDecoration: 'underline',
                        }}
                        title={isAr ? 'عرض ملف المستأجر' : 'View Profile'}
                      >
                        {linkedTenant?.fullName || c.tenantName}
                      </span>
                      <span
                        style={{
                          backgroundColor: '#e0f2fe',
                          color: '#0284c7',
                          fontSize: '11px',
                          fontWeight: 700,
                          padding: '2px 8px',
                          borderRadius: '6px',
                        }}
                      >
                        {isAr ? `متبقي ${daysLeft} يوم` : `${daysLeft}d left`}
                      </span>
                    </div>
                    <div style={{ fontSize: '12px', color: '#64748b' }}>
                      {prop?.name} • {isAr ? `وحدة ${unit?.unitNumber}` : `Unit ${unit?.unitNumber}`} • {isAr ? 'ينتهي في:' : 'Ends on:'} {formatUIDate(c.endDate)}
                    </div>
                  </div>

                  <div style={{ textAlign: isAr ? 'left' : 'right', marginLeft: '12px', marginRight: '12px' }}>
                    <div style={{ fontSize: '15px', fontWeight: 800, color: '#1e293b' }}>
                      {formatCurrency(c.monthlyRent, language)}
                    </div>
                    <span style={{ fontSize: '11px', color: '#0284c7', fontWeight: 700 }}>
                      {isAr ? 'تجديد العقد' : 'Renew'}
                    </span>
                  </div>
                  <ChevronIcon size={18} color="#94a3b8" />
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
};
