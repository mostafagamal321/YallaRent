import React, { useState, useMemo } from 'react';
import { useApp } from '../context/AppContext';
import { Property, Unit, Contract, PropertyType } from '../types';
import { formatCurrency } from '../utils/number';
import { formatUIDate, getDaysFromToday } from '../utils/date';
import {
  Building2,
  Home,
  Store,
  Plus,
  MoreVertical,
  Edit2,
  Trash2,
  FileText,
  User,
  ChevronDown,
  ChevronUp,
  Search,
  Filter,
} from 'lucide-react';

interface PropertiesScreenProps {
  onOpenContractModal: (unitId?: string, existingContract?: Contract) => void;
  onOpenUnitDetail: (unitId: string) => void;
}

export const PropertiesScreen: React.FC<PropertiesScreenProps> = ({
  onOpenContractModal,
  onOpenUnitDetail,
}) => {
  const {
    properties,
    units,
    contracts,
    tenants,
    addTenant,
    setSelectedTenantId,
    language,
    searchQuery,
    selectedPropertyId,
    setSelectedPropertyId,
    addProperty,
    updateProperty,
    deleteProperty,
    addUnit,
    updateUnit,
    deleteUnit,
    updateContractRentForUnit,
  } = useApp();

  const isAr = language === 'ar';

  // Filters & State
  const [filterStatus, setFilterStatus] = useState<string>('all');
  const [expandedPropertyId, setExpandedPropertyId] = useState<string | null>(selectedPropertyId || properties[0]?.id || null);

  // Property Modal State
  const [propertyModalOpen, setPropertyModalOpen] = useState(false);
  const [editingProperty, setEditingProperty] = useState<Property | null>(null);
  const [propName, setPropName] = useState('');
  const [propType, setPropType] = useState<PropertyType>('apartments');
  const [propAddress, setPropAddress] = useState('');

  // Unit Modal State
  const [unitModalOpen, setUnitModalOpen] = useState(false);
  const [editingUnit, setEditingUnit] = useState<Unit | null>(null);
  const [targetPropertyId, setTargetPropertyId] = useState('');
  const [unitNumber, setUnitNumber] = useState('');
  const [unitDesc, setUnitDesc] = useState('');
  const [unitNotes, setUnitNotes] = useState('');
  const [unitRent, setUnitRent] = useState<string>('');

  // Quick Rent Edit Modal State
  const [quickRentUnit, setQuickRentUnit] = useState<Unit | null>(null);
  const [quickRentValue, setQuickRentValue] = useState<string>('');

  // Map of active contracts by unitId
  const activeContractsByUnitId = useMemo(() => {
    const map = new Map<string, Contract>();
    for (const c of contracts) {
      if (!c.isHistory) {
        map.set(c.unitId, c);
      }
    }
    return map;
  }, [contracts]);

  // Determine badge for unit
  const getUnitStatusInfo = (unit: Unit) => {
    const contract = activeContractsByUnitId.get(unit.id);
    if (!contract) {
      return {
        status: 'vacant',
        label: isAr ? 'شاغرة' : 'Vacant',
        bg: '#f1f5f9',
        color: '#475569',
        border: '#cbd5e1',
      };
    }

    const daysLeft = getDaysFromToday(contract.endDate);
    if (daysLeft < 0) {
      return {
        status: 'expired',
        label: isAr ? 'منتهي' : 'Expired',
        bg: '#fee2e2',
        color: '#b91c1c',
        border: '#fca5a5',
      };
    } else if (daysLeft <= 60) {
      return {
        status: 'expiring_soon',
        label: isAr ? 'ينتهي قريباً' : 'Expiring Soon',
        bg: '#ffedd5',
        color: '#c2410c',
        border: '#fdba74',
      };
    } else {
      return {
        status: 'active',
        label: isAr ? 'ساري' : 'Active',
        bg: '#dcfce7',
        color: '#15803d',
        border: '#86efac',
      };
    }
  };

  // Filter properties and units by search query and status
  const filteredProperties = useMemo(() => {
    return properties.map((prop) => {
      let propUnits = units.filter((u) => u.propertyId === prop.id);

      if (filterStatus !== 'all') {
        propUnits = propUnits.filter((u) => {
          const info = getUnitStatusInfo(u);
          return info.status === filterStatus;
        });
      }

      if (searchQuery.trim()) {
        const q = searchQuery.toLowerCase().trim();
        const matchesProp = prop.name.toLowerCase().includes(q) || prop.address.toLowerCase().includes(q);
        propUnits = propUnits.filter((u) => {
          const contract = activeContractsByUnitId.get(u.id);
          const tenantMatches = contract?.tenantName.toLowerCase().includes(q);
          const unitNumberMatches = u.unitNumber.toLowerCase().includes(q);
          const noteMatches = u.notes?.toLowerCase().includes(q);
          return matchesProp || tenantMatches || unitNumberMatches || noteMatches;
        });
      }

      return {
        ...prop,
        matchedUnits: propUnits,
      };
    });
  }, [properties, units, activeContractsByUnitId, filterStatus, searchQuery, isAr]);

  // Handle open property modal
  const handleOpenPropertyModal = (prop?: Property) => {
    if (prop) {
      setEditingProperty(prop);
      setPropName(prop.name);
      setPropType(prop.type);
      setPropAddress(prop.address);
    } else {
      setEditingProperty(null);
      setPropName('');
      setPropType('apartments');
      setPropAddress('');
    }
    setPropertyModalOpen(true);
  };

  const handleSaveProperty = async () => {
    if (!propName.trim()) return;
    if (editingProperty) {
      await updateProperty({
        ...editingProperty,
        name: propName.trim(),
        type: propType,
        address: propAddress.trim(),
      });
    } else {
      await addProperty({
        name: propName.trim(),
        type: propType,
        address: propAddress.trim(),
      });
    }
    setPropertyModalOpen(false);
  };

  // Handle open unit modal
  const handleOpenUnitModal = (propertyId: string, unit?: Unit) => {
    setTargetPropertyId(propertyId);
    if (unit) {
      setEditingUnit(unit);
      setUnitNumber(unit.unitNumber);
      setUnitDesc(unit.description);
      setUnitNotes(unit.notes || '');
      const activeContract = activeContractsByUnitId.get(unit.id);
      setUnitRent(activeContract ? String(activeContract.monthlyRent) : (unit.targetRent ? String(unit.targetRent) : ''));
    } else {
      setEditingUnit(null);
      setUnitNumber('');
      setUnitDesc(isAr ? 'شقة سكنية' : 'Apartment');
      setUnitNotes('');
      setUnitRent('');
    }
    setUnitModalOpen(true);
  };

  const handleSaveUnit = async () => {
    if (!unitNumber.trim()) return;
    const rentNum = unitRent.trim() ? Number(unitRent) : undefined;
    if (editingUnit) {
      await updateUnit({
        ...editingUnit,
        unitNumber: unitNumber.trim(),
        description: unitDesc.trim(),
        notes: unitNotes.trim() || undefined,
        targetRent: rentNum,
      });

      // If unit has an active contract and rent was entered/changed, update contract rent!
      const activeContract = activeContractsByUnitId.get(editingUnit.id);
      if (activeContract && rentNum !== undefined && !isNaN(rentNum) && rentNum > 0 && rentNum !== activeContract.monthlyRent) {
        await updateContractRentForUnit(editingUnit.id, rentNum);
      }
    } else {
      await addUnit({
        propertyId: targetPropertyId,
        unitNumber: unitNumber.trim(),
        description: unitDesc.trim(),
        notes: unitNotes.trim() || undefined,
        targetRent: rentNum,
      });
    }
    setUnitModalOpen(false);
  };

  return (
    <div style={{ padding: '16px', paddingBottom: '90px' }}>
      {/* Header bar */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '14px' }}>
        <div>
          <h2 style={{ fontSize: '20px', fontWeight: 800, color: '#1e293b' }}>
            {isAr ? 'العقارات والوحدات' : 'Properties & Units'}
          </h2>
          <p style={{ fontSize: '13px', color: '#64748b' }}>
            {properties.length} {isAr ? 'عقارات' : 'Properties'} • {units.length} {isAr ? 'وحدات' : 'Units'}
          </p>
        </div>

        <button
          onClick={() => handleOpenPropertyModal()}
          style={{
            backgroundColor: '#1b4332',
            color: '#ffffff',
            border: 'none',
            borderRadius: '12px',
            padding: '10px 14px',
            fontSize: '13px',
            fontWeight: 700,
            display: 'flex',
            alignItems: 'center',
            gap: '6px',
            cursor: 'pointer',
          }}
        >
          <Plus size={18} />
          <span>{isAr ? 'إضافة عقار' : 'Add Property'}</span>
        </button>
      </div>

      {/* Filter Chips */}
      <div
        style={{
          display: 'flex',
          gap: '8px',
          overflowX: 'auto',
          paddingBottom: '8px',
          marginBottom: '16px',
        }}
      >
        {[
          { id: 'all', labelAr: 'الكل', labelEn: 'All' },
          { id: 'active', labelAr: 'ساري (مؤجر)', labelEn: 'Active' },
          { id: 'vacant', labelAr: 'شاغرة', labelEn: 'Vacant' },
          { id: 'expiring_soon', labelAr: 'ينتهي قريباً', labelEn: 'Expiring Soon' },
          { id: 'expired', labelAr: 'منتهي', labelEn: 'Expired' },
        ].map((chip) => (
          <button
            key={chip.id}
            onClick={() => setFilterStatus(chip.id)}
            style={{
              padding: '6px 14px',
              borderRadius: '20px',
              border: filterStatus === chip.id ? '1px solid #1b4332' : '1px solid #cbd5e1',
              backgroundColor: filterStatus === chip.id ? '#1b4332' : '#ffffff',
              color: filterStatus === chip.id ? '#ffffff' : '#475569',
              fontSize: '12px',
              fontWeight: 700,
              whiteSpace: 'nowrap',
              cursor: 'pointer',
            }}
          >
            {isAr ? chip.labelAr : chip.labelEn}
          </button>
        ))}
      </div>

      {/* Properties List */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
        {filteredProperties.map((prop) => {
          const isExpanded = expandedPropertyId === prop.id;
          const isShop = prop.type === 'shops';
          const totalUnitsInProp = units.filter((u) => u.propertyId === prop.id).length;

          return (
            <div
              key={prop.id}
              style={{
                backgroundColor: '#ffffff',
                borderRadius: '16px',
                border: '1px solid #e2e8f0',
                overflow: 'hidden',
                boxShadow: '0 2px 8px rgba(0,0,0,0.04)',
              }}
            >
              {/* Property Card Header */}
              <div
                style={{
                  padding: '14px 16px',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  backgroundColor: isExpanded ? '#f8fafc' : '#ffffff',
                  borderBottom: isExpanded ? '1px solid #e2e8f0' : 'none',
                  cursor: 'pointer',
                }}
                onClick={() => setExpandedPropertyId(isExpanded ? null : prop.id)}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '12px', flex: 1 }}>
                  <div
                    style={{
                      width: '42px',
                      height: '42px',
                      borderRadius: '10px',
                      backgroundColor: isShop ? '#fef3c7' : '#dcfce7',
                      color: isShop ? '#b45309' : '#15803d',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                    }}
                  >
                    {isShop ? <Store size={22} /> : <Home size={22} />}
                  </div>
                  <div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                      <h3 style={{ fontSize: '16px', fontWeight: 800, color: '#1e293b', margin: 0 }}>
                        {prop.name}
                      </h3>
                      <span
                        style={{
                          fontSize: '11px',
                          padding: '1px 6px',
                          borderRadius: '4px',
                          backgroundColor: isShop ? '#fef3c7' : '#e0e7ff',
                          color: isShop ? '#92400e' : '#3730a3',
                          fontWeight: 700,
                        }}
                      >
                        {isShop ? (isAr ? 'محلات' : 'Shops') : (isAr ? 'عمارة سكنية' : 'Apartments')}
                      </span>
                    </div>
                    <p style={{ fontSize: '12px', color: '#64748b', margin: '2px 0 0 0' }}>
                      {prop.address} • {totalUnitsInProp} {isAr ? 'وحدات' : 'units'}
                    </p>
                  </div>
                </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }} onClick={(e) => e.stopPropagation()}>
                  <button
                    onClick={() => handleOpenPropertyModal(prop)}
                    style={{
                      width: '34px',
                      height: '34px',
                      borderRadius: '8px',
                      border: '1px solid #e2e8f0',
                      backgroundColor: '#ffffff',
                      color: '#475569',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      cursor: 'pointer',
                    }}
                    title={isAr ? 'تعديل العقار' : 'Edit Property'}
                  >
                    <Edit2 size={16} />
                  </button>

                  <button
                    onClick={() => setExpandedPropertyId(isExpanded ? null : prop.id)}
                    style={{
                      width: '34px',
                      height: '34px',
                      borderRadius: '8px',
                      border: 'none',
                      backgroundColor: '#f1f5f9',
                      color: '#475569',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      cursor: 'pointer',
                    }}
                  >
                    {isExpanded ? <ChevronUp size={18} /> : <ChevronDown size={18} />}
                  </button>
                </div>
              </div>

              {/* Units section when expanded */}
              {isExpanded && (
                <div style={{ padding: '14px 16px' }}>
                  <div
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      marginBottom: '12px',
                    }}
                  >
                    <span style={{ fontSize: '13px', fontWeight: 800, color: '#334155' }}>
                      {isAr ? 'الوحدات والمستأجرين' : 'Units & Tenants'} ({prop.matchedUnits.length})
                    </span>
                    <button
                      onClick={() => handleOpenUnitModal(prop.id)}
                      style={{
                        backgroundColor: '#f0fdf4',
                        color: '#166534',
                        border: '1px solid #bbf7d0',
                        borderRadius: '8px',
                        padding: '6px 12px',
                        fontSize: '12px',
                        fontWeight: 700,
                        display: 'flex',
                        alignItems: 'center',
                        gap: '4px',
                        cursor: 'pointer',
                      }}
                    >
                      <Plus size={14} />
                      <span>{isAr ? 'إضافة وحدة' : 'Add Unit'}</span>
                    </button>
                  </div>

                  {prop.matchedUnits.length === 0 ? (
                    <div
                      style={{
                        textAlign: 'center',
                        padding: '20px',
                        color: '#94a3b8',
                        fontSize: '13px',
                      }}
                    >
                      {isAr ? 'لا توجد وحدات تطابق التصفية الحالية' : 'No units match current filter'}
                    </div>
                  ) : (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                      {prop.matchedUnits.map((unit) => {
                        const contract = activeContractsByUnitId.get(unit.id);
                        const statusInfo = getUnitStatusInfo(unit);
                        const linkedTenant = contract
                          ? (contract.tenantId
                              ? tenants.find((t) => t.id === contract.tenantId)
                              : tenants.find((t) => t.fullName.trim() === contract.tenantName.trim()))
                          : undefined;

                        return (
                          <div
                            key={unit.id}
                            style={{
                              border: '1px solid #e2e8f0',
                              borderRadius: '12px',
                              padding: '12px 14px',
                              backgroundColor: '#ffffff',
                              display: 'flex',
                              alignItems: 'center',
                              justifyContent: 'space-between',
                              transition: 'box-shadow 0.15s ease',
                            }}
                          >
                            <div
                              style={{ flex: 1, cursor: 'pointer' }}
                              onClick={() => onOpenUnitDetail(unit.id)}
                            >
                              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px' }}>
                                <span
                                  style={{
                                    fontSize: '14px',
                                    fontWeight: 800,
                                    color: '#0f172a',
                                  }}
                                >
                                  {isAr ? `وحدة ${unit.unitNumber}` : `Unit ${unit.unitNumber}`}
                                </span>
                                <span
                                  style={{
                                    backgroundColor: statusInfo.bg,
                                    color: statusInfo.color,
                                    border: `1px solid ${statusInfo.border}`,
                                    fontSize: '11px',
                                    fontWeight: 700,
                                    padding: '2px 8px',
                                    borderRadius: '6px',
                                  }}
                                >
                                  {statusInfo.label}
                                </span>
                              </div>

                              {contract ? (
                                <div
                                  style={{ display: 'flex', alignItems: 'center', gap: '10px', marginTop: '4px', cursor: 'pointer' }}
                                  onClick={async (e) => {
                                    e.stopPropagation();
                                    if (linkedTenant) {
                                      setSelectedTenantId(linkedTenant.id);
                                    } else if (contract.tenantName) {
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
                                >
                                  <div
                                    style={{
                                      width: '32px',
                                      height: '32px',
                                      borderRadius: '50%',
                                      backgroundColor: '#e2e8f0',
                                      overflow: 'hidden',
                                      border: '1.5px solid #1b4332',
                                      display: 'flex',
                                      alignItems: 'center',
                                      justifyContent: 'center',
                                      flexShrink: 0,
                                      cursor: linkedTenant ? 'pointer' : 'default',
                                    }}
                                    title={isAr ? 'عرض ملف المستأجر' : 'View Tenant Profile'}
                                  >
                                    {linkedTenant?.photoPath ? (
                                      <img
                                        src={linkedTenant.photoPath}
                                        alt={linkedTenant.fullName}
                                        style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                                      />
                                    ) : (
                                      <User size={18} color="#94a3b8" />
                                    )}
                                  </div>
                                  <div>
                                    <div
                                      style={{
                                        fontSize: '14px',
                                        fontWeight: 800,
                                        color: '#1b4332',
                                        textDecoration: linkedTenant ? 'underline' : 'none',
                                      }}
                                    >
                                      {linkedTenant?.fullName || contract.tenantName}
                                    </div>
                                    <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginTop: '2px', flexWrap: 'wrap' }}>
                                      <span style={{ fontSize: '12px', fontWeight: 800, color: '#1b4332' }}>
                                        {formatCurrency(contract.monthlyRent, language)}
                                      </span>
                                      <button
                                        type="button"
                                        onClick={(e) => {
                                          e.stopPropagation();
                                          setQuickRentUnit(unit);
                                          setQuickRentValue(String(contract.monthlyRent));
                                        }}
                                        style={{
                                          background: '#ecfdf5',
                                          color: '#065f46',
                                          border: '1px solid #a7f3d0',
                                          borderRadius: '6px',
                                          padding: '2px 7px',
                                          fontSize: '11px',
                                          fontWeight: 700,
                                          cursor: 'pointer',
                                          display: 'inline-flex',
                                          alignItems: 'center',
                                          gap: '3px',
                                        }}
                                        title={isAr ? 'تعديل قيمة الإيجار مباشرة' : 'Edit Rent'}
                                      >
                                        <Edit2 size={11} />
                                        <span>{isAr ? 'تعديل الإيجار' : 'Edit Rent'}</span>
                                      </button>
                                      <span style={{ fontSize: '11px', color: '#94a3b8' }}>•</span>
                                      <span style={{ fontSize: '11px', color: '#64748b' }}>
                                        {isAr ? 'ينتهي:' : 'Ends:'} {formatUIDate(contract.endDate)}
                                      </span>
                                    </div>
                                  </div>
                                </div>
                              ) : (
                                <div style={{ fontSize: '13px', color: '#64748b' }}>
                                  {unit.notes ? (
                                    <span style={{ color: '#0369a1', fontWeight: 600 }}>{unit.notes}</span>
                                  ) : (
                                    <span>{isAr ? 'لا يوجد عقد نشط حالياً' : 'No active lease'}</span>
                                  )}
                                  {unit.targetRent && (
                                    <div style={{ fontSize: '11px', color: '#1b4332', fontWeight: 700, marginTop: '2px' }}>
                                      {isAr ? 'الإيجار المستهدف:' : 'Target Rent:'} {formatCurrency(unit.targetRent, language)}
                                    </div>
                                  )}
                                </div>
                              )}
                            </div>

                            {/* Actions */}
                            <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                              {contract ? (
                                <>
                                  <button
                                    onClick={() => onOpenUnitDetail(unit.id)}
                                    style={{
                                      backgroundColor: '#1b4332',
                                      color: '#ffffff',
                                      border: 'none',
                                      borderRadius: '8px',
                                      padding: '8px 10px',
                                      fontSize: '12px',
                                      fontWeight: 700,
                                      cursor: 'pointer',
                                    }}
                                  >
                                    {isAr ? 'التفاصيل' : 'Details'}
                                  </button>

                                  <button
                                    onClick={() => onOpenContractModal(unit.id, contract)}
                                    style={{
                                      backgroundColor: '#f0fdf4',
                                      color: '#166534',
                                      border: '1px solid #bbf7d0',
                                      borderRadius: '8px',
                                      padding: '8px 10px',
                                      fontSize: '12px',
                                      fontWeight: 700,
                                      cursor: 'pointer',
                                      display: 'flex',
                                      alignItems: 'center',
                                      gap: '4px',
                                    }}
                                    title={isAr ? 'تعديل بيانات العقد والإيجار' : 'Edit Lease & Rent'}
                                  >
                                    <Edit2 size={13} />
                                    <span>{isAr ? 'تعديل العقد' : 'Edit Lease'}</span>
                                  </button>
                                </>
                              ) : (
                                <button
                                  onClick={() => onOpenContractModal(unit.id)}
                                  style={{
                                    backgroundColor: '#2563eb',
                                    color: '#ffffff',
                                    border: 'none',
                                    borderRadius: '8px',
                                    padding: '8px 12px',
                                    fontSize: '12px',
                                    fontWeight: 700,
                                    cursor: 'pointer',
                                  }}
                                >
                                  {isAr ? 'إنشاء عقد' : 'Add Lease'}
                                </button>
                              )}

                              <button
                                onClick={() => handleOpenUnitModal(prop.id, unit)}
                                style={{
                                  width: '32px',
                                  height: '32px',
                                  borderRadius: '8px',
                                  border: '1px solid #e2e8f0',
                                  backgroundColor: '#ffffff',
                                  color: '#64748b',
                                  display: 'flex',
                                  alignItems: 'center',
                                  justifyContent: 'center',
                                  cursor: 'pointer',
                                }}
                                title={isAr ? 'تعديل الوحدة' : 'Edit Unit'}
                              >
                                <Edit2 size={14} />
                              </button>

                              <button
                                onClick={async (e) => {
                                  e.stopPropagation();
                                  if (confirm(isAr ? `هل أنت متأكد من حذف وحدة ${unit.unitNumber}؟` : `Delete unit ${unit.unitNumber}?`)) {
                                    await deleteUnit(unit.id);
                                  }
                                }}
                                style={{
                                  width: '32px',
                                  height: '32px',
                                  borderRadius: '8px',
                                  border: '1px solid #fee2e2',
                                  backgroundColor: '#fef2f2',
                                  color: '#dc2626',
                                  display: 'flex',
                                  alignItems: 'center',
                                  justifyContent: 'center',
                                  cursor: 'pointer',
                                }}
                                title={isAr ? 'حذف الوحدة' : 'Delete Unit'}
                              >
                                <Trash2 size={14} />
                              </button>
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  )}
                </div>
              )}
            </div>
          );
        })}
      </div>

      {/* Property Modal */}
      {propertyModalOpen && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            backgroundColor: 'rgba(0,0,0,0.5)',
            zIndex: 100,
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
              maxWidth: '440px',
              width: '100%',
              boxShadow: '0 10px 25px rgba(0,0,0,0.15)',
            }}
          >
            <h3 style={{ fontSize: '18px', fontWeight: 800, marginBottom: '16px', color: '#1e293b' }}>
              {editingProperty
                ? isAr
                  ? 'تعديل بيانات العقار'
                  : 'Edit Property'
                : isAr
                ? 'إضافة عقار جديد'
                : 'Add New Property'}
            </h3>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '14px', marginBottom: '20px' }}>
              <div>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 700, color: '#475569', marginBottom: '4px' }}>
                  {isAr ? 'اسم العقار' : 'Property Name'}
                </label>
                <input
                  type="text"
                  value={propName}
                  onChange={(e) => setPropName(e.target.value)}
                  placeholder={isAr ? 'مثال: ع مسجد الغفور' : 'e.g. Building 1'}
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
                  {isAr ? 'نوع العقار' : 'Property Type'}
                </label>
                <div style={{ display: 'flex', gap: '10px' }}>
                  <button
                    type="button"
                    onClick={() => setPropType('apartments')}
                    style={{
                      flex: 1,
                      padding: '10px',
                      borderRadius: '8px',
                      border: propType === 'apartments' ? '2px solid #1b4332' : '1px solid #cbd5e1',
                      backgroundColor: propType === 'apartments' ? '#dcfce7' : '#ffffff',
                      color: propType === 'apartments' ? '#14532d' : '#475569',
                      fontWeight: 700,
                      cursor: 'pointer',
                    }}
                  >
                    {isAr ? 'عمارة شقق' : 'Apartments'}
                  </button>
                  <button
                    type="button"
                    onClick={() => setPropType('shops')}
                    style={{
                      flex: 1,
                      padding: '10px',
                      borderRadius: '8px',
                      border: propType === 'shops' ? '2px solid #1b4332' : '1px solid #cbd5e1',
                      backgroundColor: propType === 'shops' ? '#fef3c7' : '#ffffff',
                      color: propType === 'shops' ? '#92400e' : '#475569',
                      fontWeight: 700,
                      cursor: 'pointer',
                    }}
                  >
                    {isAr ? 'محلات تجارية' : 'Shops'}
                  </button>
                </div>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 700, color: '#475569', marginBottom: '4px' }}>
                  {isAr ? 'عنوان العقار (يُستخدم في العقد)' : 'Property Address (Used in Contract)'}
                </label>
                <textarea
                  value={propAddress}
                  onChange={(e) => setPropAddress(e.target.value)}
                  placeholder={isAr ? 'الفيوم – الجون – ش أبو بكر الصديق...' : 'Address...'}
                  rows={3}
                  style={{
                    width: '100%',
                    padding: '8px 12px',
                    borderRadius: '8px',
                    border: '1px solid #cbd5e1',
                    fontSize: '13px',
                    boxSizing: 'border-box',
                    fontFamily: 'inherit',
                  }}
                />
              </div>
            </div>

            <div style={{ display: 'flex', gap: '10px', alignItems: 'center', justifyContent: 'space-between' }}>
              {editingProperty && (
                <button
                  type="button"
                  onClick={async () => {
                    if (
                      confirm(
                        isAr
                          ? `هل أنت متأكد من حذف عقار "${editingProperty.name}" وكافة وحداته وعقوده؟`
                          : `Are you sure you want to delete property "${editingProperty.name}" and all its units?`
                      )
                    ) {
                      await deleteProperty(editingProperty.id);
                      setPropertyModalOpen(false);
                    }
                  }}
                  style={{
                    padding: '10px 14px',
                    borderRadius: '10px',
                    border: '1px solid #fee2e2',
                    backgroundColor: '#fef2f2',
                    color: '#dc2626',
                    fontWeight: 700,
                    fontSize: '12px',
                    cursor: 'pointer',
                  }}
                >
                  {isAr ? 'حذف العقار' : 'Delete Property'}
                </button>
              )}
              <div style={{ display: 'flex', gap: '10px', marginRight: isAr ? 'auto' : 0, marginLeft: isAr ? 0 : 'auto' }}>
                <button
                  type="button"
                  onClick={() => setPropertyModalOpen(false)}
                  style={{
                    padding: '10px 18px',
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
                  onClick={handleSaveProperty}
                  style={{
                    padding: '10px 22px',
                    borderRadius: '10px',
                    border: 'none',
                    backgroundColor: '#1b4332',
                    color: '#ffffff',
                    fontWeight: 700,
                    cursor: 'pointer',
                  }}
                >
                  {isAr ? 'حفظ العقار' : 'Save Property'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Unit Modal */}
      {unitModalOpen && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            backgroundColor: 'rgba(0,0,0,0.5)',
            zIndex: 100,
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
              maxWidth: '440px',
              width: '100%',
              boxShadow: '0 10px 25px rgba(0,0,0,0.15)',
            }}
          >
            <h3 style={{ fontSize: '18px', fontWeight: 800, marginBottom: '16px', color: '#1e293b' }}>
              {editingUnit
                ? isAr
                  ? 'تعديل بيانات الوحدة'
                  : 'Edit Unit'
                : isAr
                ? 'إضافة وحدة جديدة'
                : 'Add New Unit'}
            </h3>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '14px', marginBottom: '20px' }}>
              <div>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 700, color: '#475569', marginBottom: '4px' }}>
                  {isAr ? 'رقم أو اسم الوحدة' : 'Unit Number / Identifier'}
                </label>
                <input
                  type="text"
                  value={unitNumber}
                  onChange={(e) => setUnitNumber(e.target.value)}
                  placeholder={isAr ? 'مثال: 1 أو 15 أو محل 2' : 'e.g. 1, 15, or Shop 2'}
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
                  {isAr ? 'بيان ووصف العين (المكان المؤجر)' : 'Unit Description'}
                </label>
                <input
                  type="text"
                  value={unitDesc}
                  onChange={(e) => setUnitDesc(e.target.value)}
                  placeholder={isAr ? 'مثال: شقه ٣ حجرات وصالة' : 'e.g. 3-bedroom apartment'}
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
                  {isAr ? 'ملاحظات (المستأجر المتوقع / المالك السابق)' : 'Notes (Expected tenant / owner notes)'}
                </label>
                <input
                  type="text"
                  value={unitNotes}
                  onChange={(e) => setUnitNotes(e.target.value)}
                  placeholder={isAr ? 'مثال: المستأجر السابق أو المتوقع: احمد حامد' : 'Notes...'}
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
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 700, color: '#1b4332', marginBottom: '4px' }}>
                  {isAr ? 'قيمة الإيجار الشهري (ج.م)' : 'Monthly Rent (EGP)'}
                </label>
                <input
                  type="number"
                  value={unitRent}
                  onChange={(e) => setUnitRent(e.target.value)}
                  placeholder={isAr ? 'مثال: 3000' : 'e.g. 3000'}
                  style={{
                    width: '100%',
                    height: '44px',
                    padding: '0 12px',
                    borderRadius: '8px',
                    border: '2px solid #1b4332',
                    fontSize: '15px',
                    fontWeight: 800,
                    color: '#1b4332',
                    backgroundColor: '#f0fdf4',
                    boxSizing: 'border-box',
                  }}
                />
                <span style={{ display: 'block', fontSize: '11px', color: '#0369a1', marginTop: '4px', lineHeight: '1.4' }}>
                  {editingUnit && activeContractsByUnitId.has(editingUnit.id)
                    ? (isAr ? '💡 تعديل هذا المبلغ سيقوم بتحديث العقد الساري فوراً وتعديل جميع الأقساط القادمة غير المسددة' : '💡 Updating this amount immediately updates the active lease and future unpaid bills')
                    : (isAr ? 'القيمة الإيجارية المستهدفة لهذه الوحدة' : 'Target rent for this unit')}
                </span>
              </div>
            </div>

            <div style={{ display: 'flex', gap: '10px', alignItems: 'center', justifyContent: 'space-between' }}>
              {editingUnit && (
                <button
                  type="button"
                  onClick={async () => {
                    if (confirm(isAr ? `هل أنت متأكد من حذف وحدة "${editingUnit.unitNumber}"؟` : `Delete unit "${editingUnit.unitNumber}"?`)) {
                      await deleteUnit(editingUnit.id);
                      setUnitModalOpen(false);
                    }
                  }}
                  style={{
                    padding: '10px 14px',
                    borderRadius: '10px',
                    border: '1px solid #fee2e2',
                    backgroundColor: '#fef2f2',
                    color: '#dc2626',
                    fontWeight: 700,
                    fontSize: '12px',
                    cursor: 'pointer',
                  }}
                >
                  {isAr ? 'حذف الوحدة' : 'Delete Unit'}
                </button>
              )}
              <div style={{ display: 'flex', gap: '10px', marginRight: isAr ? 'auto' : 0, marginLeft: isAr ? 0 : 'auto' }}>
                <button
                  type="button"
                  onClick={() => setUnitModalOpen(false)}
                  style={{
                    padding: '10px 18px',
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
                  onClick={handleSaveUnit}
                  style={{
                    padding: '10px 22px',
                    borderRadius: '10px',
                    border: 'none',
                    backgroundColor: '#1b4332',
                    color: '#ffffff',
                    fontWeight: 700,
                    cursor: 'pointer',
                  }}
                >
                  {isAr ? 'حفظ الوحدة' : 'Save Unit'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Quick Rent Modal */}
      {quickRentUnit && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            backgroundColor: 'rgba(0,0,0,0.6)',
            zIndex: 300,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: '20px',
          }}
          onClick={() => setQuickRentUnit(null)}
        >
          <div
            style={{
              backgroundColor: '#ffffff',
              borderRadius: '20px',
              maxWidth: '420px',
              width: '100%',
              padding: '22px',
              boxShadow: '0 20px 25px -5px rgba(0,0,0,0.25)',
            }}
            onClick={(e) => e.stopPropagation()}
          >
            <h3 style={{ fontSize: '18px', fontWeight: 800, color: '#1e293b', margin: '0 0 6px 0' }}>
              {isAr ? `تعديل قيمة الإيجار الشهري (وحدة ${quickRentUnit.unitNumber})` : `Edit Monthly Rent (Unit ${quickRentUnit.unitNumber})`}
            </h3>
            <p style={{ fontSize: '12px', color: '#64748b', margin: '0 0 16px 0', lineHeight: '1.5' }}>
              {isAr ? 'سيتم تطبيق القيمة الجديدة فوراً على العقد الساري وتحديث كافة الأقساط القادمة غير المسددة' : 'New amount will be applied immediately to the active contract and upcoming unpaid bills'}
            </p>

            <div style={{ marginBottom: '20px' }}>
              <label style={{ display: 'block', fontSize: '13px', fontWeight: 700, color: '#334155', marginBottom: '6px' }}>
                {isAr ? 'قيمة الإيجار الشهري الجديدة (ج.م)' : 'New Monthly Rent (EGP)'}
              </label>
              <input
                type="number"
                autoFocus
                value={quickRentValue}
                onChange={(e) => setQuickRentValue(e.target.value)}
                style={{
                  width: '100%',
                  height: '46px',
                  padding: '0 12px',
                  borderRadius: '10px',
                  border: '2px solid #1b4332',
                  fontSize: '16px',
                  fontWeight: 800,
                  color: '#1b4332',
                  backgroundColor: '#f0fdf4',
                  boxSizing: 'border-box',
                }}
              />
            </div>

            <div style={{ display: 'flex', gap: '10px', justifyContent: 'flex-end' }}>
              <button
                type="button"
                onClick={() => setQuickRentUnit(null)}
                style={{
                  padding: '10px 16px',
                  borderRadius: '10px',
                  border: '1px solid #cbd5e1',
                  backgroundColor: '#ffffff',
                  color: '#475569',
                  fontWeight: 700,
                  fontSize: '13px',
                  cursor: 'pointer',
                }}
              >
                {isAr ? 'إلغاء' : 'Cancel'}
              </button>
              <button
                type="button"
                onClick={async () => {
                  const num = Number(quickRentValue);
                  if (num > 0 && quickRentUnit) {
                    await updateContractRentForUnit(quickRentUnit.id, num);
                    setQuickRentUnit(null);
                  }
                }}
                style={{
                  padding: '10px 20px',
                  borderRadius: '10px',
                  border: 'none',
                  backgroundColor: '#1b4332',
                  color: '#ffffff',
                  fontWeight: 800,
                  fontSize: '13px',
                  cursor: 'pointer',
                }}
              >
                {isAr ? 'حفظ وتحديث الإيجار' : 'Save & Update Rent'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
