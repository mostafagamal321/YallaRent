import React, { useState, useMemo } from 'react';
import { useApp } from '../context/AppContext';
import { Tenant } from '../types';
import { formatCurrency, toArabicIndicDigits } from '../utils/number';
import {
  Users,
  Search,
  Plus,
  Phone,
  MessageCircle,
  Building,
  Home,
  User,
  Filter,
} from 'lucide-react';

interface TenantsScreenProps {
  onSelectTenant: (tenantId: string) => void;
  onAddTenant: () => void;
}

export const TenantsScreen: React.FC<TenantsScreenProps> = ({
  onSelectTenant,
  onAddTenant,
}) => {
  const { tenants, contracts, properties, units, language } = useApp();
  const isAr = language === 'ar';

  const [search, setSearch] = useState('');
  const [selectedBuildingId, setSelectedBuildingId] = useState<string>('all');
  const [selectedStatus, setSelectedStatus] = useState<string>('all'); // 'all' | 'active' | 'expiring_soon' | 'expired' | 'no_contract'

  // Map tenants to their active contract, unit, and property
  const tenantDetails = useMemo(() => {
    return tenants.map((tenant) => {
      const tenantContracts = contracts.filter(
        (c) => c.tenantId === tenant.id || c.tenantName === tenant.fullName
      );
      const activeContract =
        tenantContracts.find((c) => !c.isHistory && (c.status === 'active' || c.status === 'expiring_soon')) ||
        tenantContracts[0];

      const unit = activeContract ? units.find((u) => u.id === activeContract.unitId) : undefined;
      const property = unit ? properties.find((p) => p.id === unit.propertyId) : undefined;

      return {
        tenant,
        activeContract,
        unit,
        property,
        contractsCount: tenantContracts.length,
      };
    });
  }, [tenants, contracts, properties, units]);

  // Filtered list
  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    return tenantDetails.filter((item) => {
      const { tenant, activeContract, unit, property } = item;

      // Building filter
      if (selectedBuildingId !== 'all') {
        if (!property || property.id !== selectedBuildingId) return false;
      }

      // Status filter
      if (selectedStatus !== 'all') {
        if (selectedStatus === 'no_contract') {
          if (activeContract) return false;
        } else if (selectedStatus === 'active') {
          if (!activeContract || activeContract.status !== 'active') return false;
        } else if (selectedStatus === 'expiring_soon') {
          if (!activeContract || activeContract.status !== 'expiring_soon') return false;
        } else if (selectedStatus === 'expired') {
          if (!activeContract || (activeContract.status !== 'expired' && !activeContract.isHistory)) return false;
        }
      }

      // Search query filter (name, national ID, phone, building, unit)
      if (q) {
        const nameMatch = tenant.fullName.toLowerCase().includes(q);
        const idMatch = tenant.nationalId ? tenant.nationalId.includes(q) : false;
        const phoneMatch = tenant.phone ? tenant.phone.includes(q) : false;
        const buildingMatch = property ? property.name.toLowerCase().includes(q) : false;
        const unitMatch = unit ? unit.unitNumber.toLowerCase().includes(q) : false;

        if (!nameMatch && !idMatch && !phoneMatch && !buildingMatch && !unitMatch) {
          return false;
        }
      }

      return true;
    });
  }, [tenantDetails, search, selectedBuildingId, selectedStatus]);

  return (
    <div style={{ padding: '16px', maxWidth: '800px', margin: '0 auto' }}>
      {/* Header bar */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          marginBottom: '16px',
        }}
      >
        <div>
          <h1 style={{ fontSize: '22px', fontWeight: 900, color: '#1b4332', margin: 0 }}>
            {isAr ? 'دليل المستأجرين' : 'Tenants Directory'}
          </h1>
          <p style={{ fontSize: '13px', color: '#64748b', margin: '3px 0 0 0' }}>
            {isAr
              ? `إجمالي المستأجرين المسجلين: ${tenants.length}`
              : `Total registered tenants: ${tenants.length}`}
          </p>
        </div>

        <button
          onClick={onAddTenant}
          style={{
            padding: '10px 16px',
            borderRadius: '12px',
            backgroundColor: '#1b4332',
            color: '#ffffff',
            border: 'none',
            fontSize: '13px',
            fontWeight: 800,
            display: 'flex',
            alignItems: 'center',
            gap: '6px',
            cursor: 'pointer',
            boxShadow: '0 4px 10px rgba(27,67,50,0.2)',
          }}
        >
          <Plus size={18} />
          <span>{isAr ? 'إضافة مستأجر' : 'Add Tenant'}</span>
        </button>
      </div>

      {/* Search Input */}
      <div style={{ position: 'relative', marginBottom: '12px' }}>
        <input
          type="text"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder={
            isAr
              ? 'بحث بالاسم، الرقم القومي، الهاتف، العمارة، أو رقم الوحدة...'
              : 'Search by name, ID, phone, building, unit...'
          }
          style={{
            width: '100%',
            height: '46px',
            paddingRight: isAr ? '42px' : '14px',
            paddingLeft: isAr ? '14px' : '42px',
            borderRadius: '14px',
            border: '1.5px solid #cbd5e1',
            fontSize: '14px',
            backgroundColor: '#ffffff',
            boxSizing: 'border-box',
          }}
        />
        <div
          style={{
            position: 'absolute',
            top: '50%',
            transform: 'translateY(-50%)',
            right: isAr ? '14px' : 'auto',
            left: isAr ? 'auto' : '14px',
            pointerEvents: 'none',
            display: 'flex',
          }}
        >
          <Search size={18} color="#94a3b8" />
        </div>
      </div>

      {/* Filters (Building & Contract Status) */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: '1fr 1fr',
          gap: '10px',
          marginBottom: '16px',
        }}
      >
        {/* Building Filter */}
        <select
          value={selectedBuildingId}
          onChange={(e) => setSelectedBuildingId(e.target.value)}
          style={{
            height: '40px',
            borderRadius: '10px',
            border: '1px solid #cbd5e1',
            backgroundColor: '#ffffff',
            padding: '0 10px',
            fontSize: '12px',
            fontWeight: 700,
            color: '#334155',
          }}
        >
          <option value="all">{isAr ? '🏢 جميع العقارات والعمارات' : '🏢 All Properties'}</option>
          {properties.map((p) => (
            <option key={p.id} value={p.id}>
              {p.name}
            </option>
          ))}
        </select>

        {/* Contract Status Filter */}
        <select
          value={selectedStatus}
          onChange={(e) => setSelectedStatus(e.target.value)}
          style={{
            height: '40px',
            borderRadius: '10px',
            border: '1px solid #cbd5e1',
            backgroundColor: '#ffffff',
            padding: '0 10px',
            fontSize: '12px',
            fontWeight: 700,
            color: '#334155',
          }}
        >
          <option value="all">{isAr ? '📋 جميع حالات العقود' : '📋 All Contract Statuses'}</option>
          <option value="active">{isAr ? '🟢 عقد ساري' : '🟢 Active Contract'}</option>
          <option value="expiring_soon">{isAr ? '🟡 أوشك على الانتهاء' : '🟡 Expiring Soon'}</option>
          <option value="expired">{isAr ? '⚪ عقد منتهي' : '⚪ Expired Contract'}</option>
          <option value="no_contract">{isAr ? '⚪ بدون عقد حالي' : '⚪ No Contract'}</option>
        </select>
      </div>

      {/* Tenants List */}
      {filtered.length === 0 ? (
        <div
          style={{
            padding: '40px 20px',
            backgroundColor: '#ffffff',
            borderRadius: '18px',
            border: '1px solid #e2e8f0',
            textAlign: 'center',
          }}
        >
          <Users size={48} color="#cbd5e1" style={{ margin: '0 auto 12px auto' }} />
          <h3 style={{ fontSize: '16px', fontWeight: 800, color: '#1e293b', margin: '0 0 6px 0' }}>
            {tenants.length === 0
              ? (isAr ? 'لا يوجد مستأجرون مسجلون بعد' : 'No tenants registered yet')
              : (isAr ? 'لا توجد نتائج مطابقة لبحثك' : 'No matching tenants found')}
          </h3>
          <p style={{ fontSize: '13px', color: '#64748b', margin: '0 0 16px 0' }}>
            {tenants.length === 0
              ? (isAr ? 'ابدأ بإضافة أول ملف مستأجر أو إنشاء عقد جديد' : 'Add your first tenant profile to begin')
              : (isAr ? 'جرّب تعديل كلمات البحث أو الفلاتر' : 'Try adjusting your search or filters')}
          </p>
          {tenants.length === 0 && (
            <button
              onClick={onAddTenant}
              style={{
                padding: '10px 20px',
                borderRadius: '10px',
                backgroundColor: '#1b4332',
                color: '#ffffff',
                border: 'none',
                fontWeight: 800,
                fontSize: '13px',
                cursor: 'pointer',
              }}
            >
              {isAr ? '+ إضافة مستأجر الآن' : '+ Add Tenant Now'}
            </button>
          )}
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
          {filtered.map(({ tenant, activeContract, unit, property, contractsCount }) => (
            <div
              key={tenant.id}
              onClick={() => onSelectTenant(tenant.id)}
              style={{
                backgroundColor: '#ffffff',
                borderRadius: '16px',
                border: '1.5px solid #e2e8f0',
                padding: '14px 16px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                cursor: 'pointer',
                boxShadow: '0 2px 6px rgba(0,0,0,0.02)',
                transition: 'all 0.15s ease',
              }}
            >
              {/* Left Side: Avatar + Details */}
              <div style={{ display: 'flex', alignItems: 'center', gap: '14px', flex: 1, minWidth: 0 }}>
                {/* Round Avatar */}
                <div
                  style={{
                    width: '56px',
                    height: '56px',
                    borderRadius: '50%',
                    backgroundColor: '#e2e8f0',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    overflow: 'hidden',
                    border: '2.5px solid #1b4332',
                    flexShrink: 0,
                  }}
                >
                  {tenant.photoPath ? (
                    <img
                      src={tenant.photoPath}
                      alt={tenant.fullName}
                      style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                    />
                  ) : (
                    <User size={28} color="#94a3b8" />
                  )}
                </div>

                {/* Details */}
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <h3
                      style={{
                        fontSize: '15px',
                        fontWeight: 800,
                        color: '#1e293b',
                        margin: 0,
                        whiteSpace: 'nowrap',
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                      }}
                    >
                      {tenant.fullName}
                    </h3>
                    {contractsCount > 1 && (
                      <span
                        style={{
                          fontSize: '10px',
                          backgroundColor: '#f1f5f9',
                          color: '#475569',
                          padding: '1px 6px',
                          borderRadius: '10px',
                          fontWeight: 700,
                        }}
                      >
                        {isAr ? `${contractsCount} عقود` : `${contractsCount} contracts`}
                      </span>
                    )}
                  </div>

                  <div style={{ fontSize: '12px', color: '#64748b', marginTop: '2px' }}>
                    {property && unit ? (
                      <span>
                        {property.name} • {isAr ? `وحدة ${unit.unitNumber}` : `Unit ${unit.unitNumber}`}
                      </span>
                    ) : (
                      <span>{isAr ? 'لا يوجد وحدة مؤجرة حالياً' : 'No active unit'}</span>
                    )}
                  </div>

                  {/* Phone & National ID excerpt */}
                  <div style={{ fontSize: '11px', color: '#94a3b8', marginTop: '2px', display: 'flex', gap: '8px' }}>
                    {tenant.phone && <span>📞 {tenant.phone}</span>}
                    {tenant.nationalId && <span>🪪 ...{tenant.nationalId.slice(-4)}</span>}
                  </div>
                </div>
              </div>

              {/* Right Side: Rent & Quick Action */}
              <div style={{ textAlign: isAr ? 'left' : 'right', marginLeft: isAr ? 0 : '12px', marginRight: isAr ? '12px' : 0 }}>
                {activeContract ? (
                  <div>
                    <div style={{ fontSize: '15px', fontWeight: 800, color: '#1b4332' }}>
                      {formatCurrency(activeContract.monthlyRent, language)}
                    </div>
                    <div style={{ fontSize: '11px', color: '#64748b' }}>
                      {isAr ? 'شهرياً' : '/ month'}
                    </div>
                  </div>
                ) : (
                  <span
                    style={{
                      fontSize: '11px',
                      padding: '3px 8px',
                      borderRadius: '6px',
                      backgroundColor: '#f1f5f9',
                      color: '#64748b',
                      fontWeight: 700,
                    }}
                  >
                    {isAr ? 'بدون عقد' : 'No contract'}
                  </span>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
