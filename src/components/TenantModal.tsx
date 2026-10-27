import React, { useState } from 'react';
import { useApp } from '../context/AppContext';
import { Tenant } from '../types';
import { captureOrPickPhoto } from '../services/photoService';
import { Camera, Image as ImageIcon, X, Check, Trash2, User, CreditCard } from 'lucide-react';

interface TenantModalProps {
  tenantId?: string | null;
  initialData?: Partial<Tenant>;
  onClose: () => void;
  onSuccess?: (tenant: Tenant) => void;
}

export const TenantModal: React.FC<TenantModalProps> = ({
  tenantId,
  initialData,
  onClose,
  onSuccess,
}) => {
  const { tenants, addTenant, updateTenant, language } = useApp();
  const isAr = language === 'ar';

  const existingTenant = tenantId ? tenants.find((t) => t.id === tenantId) : undefined;

  const [fullName, setFullName] = useState(existingTenant?.fullName || initialData?.fullName || '');
  const [nationalId, setNationalId] = useState(existingTenant?.nationalId || initialData?.nationalId || '');
  const [job, setJob] = useState(existingTenant?.job || initialData?.job || (isAr ? 'موظف' : 'Employee'));
  const [phone, setPhone] = useState(existingTenant?.phone || initialData?.phone || '');
  const [address, setAddress] = useState(existingTenant?.address || initialData?.address || (isAr ? 'الفيوم' : 'Fayoum'));
  const [nationality, setNationality] = useState(existingTenant?.nationality || initialData?.nationality || (isAr ? 'مصري' : 'Egyptian'));
  const [notes, setNotes] = useState(existingTenant?.notes || initialData?.notes || '');

  const [photoPath, setPhotoPath] = useState<string | null | undefined>(
    existingTenant?.photoPath || initialData?.photoPath
  );
  const [idCardPhotoFrontPath, setIdCardPhotoFrontPath] = useState<string | null | undefined>(
    existingTenant?.idCardPhotoFrontPath || initialData?.idCardPhotoFrontPath
  );
  const [idCardPhotoBackPath, setIdCardPhotoBackPath] = useState<string | null | undefined>(
    existingTenant?.idCardPhotoBackPath || initialData?.idCardPhotoBackPath
  );

  const [showIdDocs, setShowIdDocs] = useState(false);
  const [loadingPhoto, setLoadingPhoto] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');

  const handlePickPhoto = async (
    target: 'avatar' | 'idFront' | 'idBack',
    source: 'camera' | 'photos'
  ) => {
    setLoadingPhoto(true);
    try {
      const result = await captureOrPickPhoto(source);
      if (result) {
        if (target === 'avatar') setPhotoPath(result);
        if (target === 'idFront') setIdCardPhotoFrontPath(result);
        if (target === 'idBack') setIdCardPhotoBackPath(result);
      }
    } catch (e) {
      console.error('Error selecting photo:', e);
    } finally {
      setLoadingPhoto(false);
    }
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!fullName.trim()) {
      setErrorMsg(isAr ? 'يرجى إدخال اسم المستأجر بالكامل' : 'Please enter tenant full name');
      return;
    }

    if (nationalId.trim() && nationalId.trim().length !== 14) {
      setErrorMsg(
        isAr
          ? 'الرقم القومي يجب أن يتكون من ١٤ رقماً (أو اتركه فارغاً مؤقتاً)'
          : 'National ID must be 14 digits (or leave blank)'
      );
      return;
    }

    const payload: Omit<Tenant, 'id'> = {
      fullName: fullName.trim(),
      nationalId: nationalId.trim(),
      job: job.trim() || (isAr ? 'موظف' : 'Employee'),
      phone: phone.trim(),
      address: address.trim(),
      nationality: nationality.trim() || (isAr ? 'مصري' : 'Egyptian'),
      photoPath,
      idCardPhotoFrontPath,
      idCardPhotoBackPath,
      notes: notes.trim() || undefined,
    };

    if (existingTenant) {
      const updated: Tenant = { ...payload, id: existingTenant.id };
      await updateTenant(updated);
      if (onSuccess) onSuccess(updated);
    } else {
      const created = await addTenant(payload);
      if (onSuccess) onSuccess(created);
    }
    onClose();
  };

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        backgroundColor: 'rgba(0,0,0,0.65)',
        zIndex: 260,
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
          maxWidth: '500px',
          width: '100%',
          maxHeight: '94vh',
          overflowY: 'auto',
          boxShadow: '0 12px 30px rgba(0,0,0,0.25)',
        }}
      >
        {/* Header */}
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
          <div>
            <h3 style={{ fontSize: '18px', fontWeight: 800, color: '#1e293b', margin: 0 }}>
              {existingTenant
                ? (isAr ? 'تعديل بيانات المستأجر' : 'Edit Tenant Profile')
                : (isAr ? 'إضافة مستأجر جديد' : 'Add New Tenant')}
            </h3>
            <p style={{ fontSize: '12px', color: '#64748b', margin: '2px 0 0 0' }}>
              {isAr ? 'ملف المستأجر، بيانات الهوية والصور' : 'Tenant profile, ID & contact info'}
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

        {errorMsg && (
          <div
            style={{
              padding: '10px 14px',
              backgroundColor: '#fef2f2',
              color: '#dc2626',
              borderRadius: '8px',
              fontSize: '12px',
              fontWeight: 700,
              marginBottom: '14px',
            }}
          >
            {errorMsg}
          </div>
        )}

        <form onSubmit={handleSave} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
          {/* Avatar Photo Section */}
          <div
            style={{
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              justifyContent: 'center',
              padding: '12px',
              backgroundColor: '#f8fafc',
              borderRadius: '16px',
              border: '1px dashed #cbd5e1',
            }}
          >
            <div
              style={{
                width: '90px',
                height: '90px',
                borderRadius: '50%',
                backgroundColor: '#e2e8f0',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                overflow: 'hidden',
                position: 'relative',
                border: '3px solid #1b4332',
                marginBottom: '8px',
              }}
            >
              {photoPath ? (
                <img
                  src={photoPath}
                  alt="Tenant"
                  style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                />
              ) : (
                <User size={42} color="#94a3b8" />
              )}
            </div>

            <div style={{ fontSize: '12px', fontWeight: 700, color: '#334155', marginBottom: '8px' }}>
              {isAr ? 'صورة المستأجر الشخصية' : 'Tenant Photo'}
            </div>

            <div style={{ display: 'flex', gap: '8px' }}>
              <button
                type="button"
                onClick={() => handlePickPhoto('avatar', 'camera')}
                disabled={loadingPhoto}
                style={{
                  padding: '6px 12px',
                  borderRadius: '8px',
                  backgroundColor: '#1b4332',
                  color: '#ffffff',
                  border: 'none',
                  fontSize: '11px',
                  fontWeight: 700,
                  display: 'flex',
                  alignItems: 'center',
                  gap: '4px',
                  cursor: 'pointer',
                }}
              >
                <Camera size={14} />
                <span>{isAr ? 'كاميرا' : 'Camera'}</span>
              </button>

              <button
                type="button"
                onClick={() => handlePickPhoto('avatar', 'photos')}
                disabled={loadingPhoto}
                style={{
                  padding: '6px 12px',
                  borderRadius: '8px',
                  backgroundColor: '#0284c7',
                  color: '#ffffff',
                  border: 'none',
                  fontSize: '11px',
                  fontWeight: 700,
                  display: 'flex',
                  alignItems: 'center',
                  gap: '4px',
                  cursor: 'pointer',
                }}
              >
                <ImageIcon size={14} />
                <span>{isAr ? 'المعرض' : 'Gallery'}</span>
              </button>

              {photoPath && (
                <button
                  type="button"
                  onClick={() => setPhotoPath(undefined)}
                  style={{
                    padding: '6px 10px',
                    borderRadius: '8px',
                    backgroundColor: '#fee2e2',
                    color: '#dc2626',
                    border: 'none',
                    fontSize: '11px',
                    fontWeight: 700,
                    cursor: 'pointer',
                  }}
                >
                  <Trash2 size={14} />
                </button>
              )}
            </div>
          </div>

          {/* Full Name */}
          <div>
            <label style={{ display: 'block', fontSize: '12px', fontWeight: 700, color: '#334155', marginBottom: '4px' }}>
              {isAr ? 'الاسم بالكامل (ثلاثي أو رباعي) *' : 'Full Name *'}
            </label>
            <input
              type="text"
              required
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
              placeholder={isAr ? 'مثال: محمد أحمد علي' : 'e.g. Mohamed Ahmed Ali'}
              style={{
                width: '100%',
                height: '42px',
                padding: '0 12px',
                borderRadius: '8px',
                border: '1.5px solid #cbd5e1',
                fontSize: '14px',
                fontWeight: 600,
                boxSizing: 'border-box',
              }}
            />
          </div>

          {/* National ID (14 digits) */}
          <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '4px' }}>
              <label style={{ fontSize: '12px', fontWeight: 700, color: '#334155' }}>
                {isAr ? 'الرقم القومي (١٤ رقماً)' : 'National ID (14 digits)'}
              </label>
              <span
                style={{
                  fontSize: '11px',
                  fontWeight: 700,
                  color: nationalId.length === 14 ? '#166534' : nationalId.length > 0 ? '#b45309' : '#94a3b8',
                }}
              >
                {nationalId.length} / 14
              </span>
            </div>
            <input
              type="text"
              maxLength={14}
              value={nationalId}
              onChange={(e) => setNationalId(e.target.value.replace(/\D/g, ''))}
              placeholder="29012011234567"
              style={{
                width: '100%',
                height: '42px',
                padding: '0 12px',
                borderRadius: '8px',
                border: nationalId && nationalId.length !== 14 ? '1.5px solid #f59e0b' : '1.5px solid #cbd5e1',
                fontSize: '15px',
                fontWeight: 700,
                letterSpacing: '1px',
                boxSizing: 'border-box',
              }}
            />
          </div>

          {/* Job & Phone */}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
            <div>
              <label style={{ display: 'block', fontSize: '12px', fontWeight: 700, color: '#334155', marginBottom: '4px' }}>
                {isAr ? 'المهنة (مهنة المستأجر)' : 'Job / Profession'}
              </label>
              <input
                type="text"
                value={job}
                onChange={(e) => setJob(e.target.value)}
                placeholder={isAr ? 'مهندس، مدرس، تاجر...' : 'e.g. Engineer'}
                style={{
                  width: '100%',
                  height: '42px',
                  padding: '0 12px',
                  borderRadius: '8px',
                  border: '1.5px solid #cbd5e1',
                  fontSize: '13px',
                  boxSizing: 'border-box',
                }}
              />
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '12px', fontWeight: 700, color: '#334155', marginBottom: '4px' }}>
                {isAr ? 'رقم الهاتف' : 'Phone Number'}
              </label>
              <input
                type="tel"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                placeholder="010xxxxxxxx"
                style={{
                  width: '100%',
                  height: '42px',
                  padding: '0 12px',
                  borderRadius: '8px',
                  border: '1.5px solid #cbd5e1',
                  fontSize: '13px',
                  fontWeight: 600,
                  boxSizing: 'border-box',
                }}
              />
            </div>
          </div>

          {/* Address & Nationality */}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
            <div>
              <label style={{ display: 'block', fontSize: '12px', fontWeight: 700, color: '#334155', marginBottom: '4px' }}>
                {isAr ? 'محل الإقامة (العنوان)' : 'Address'}
              </label>
              <input
                type="text"
                value={address}
                onChange={(e) => setAddress(e.target.value)}
                placeholder={isAr ? 'الفيوم' : 'Fayoum'}
                style={{
                  width: '100%',
                  height: '42px',
                  padding: '0 12px',
                  borderRadius: '8px',
                  border: '1.5px solid #cbd5e1',
                  fontSize: '13px',
                  boxSizing: 'border-box',
                }}
              />
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '12px', fontWeight: 700, color: '#334155', marginBottom: '4px' }}>
                {isAr ? 'الجنسية' : 'Nationality'}
              </label>
              <input
                type="text"
                value={nationality}
                onChange={(e) => setNationality(e.target.value)}
                placeholder={isAr ? 'مصري' : 'Egyptian'}
                style={{
                  width: '100%',
                  height: '42px',
                  padding: '0 12px',
                  borderRadius: '8px',
                  border: '1.5px solid #cbd5e1',
                  fontSize: '13px',
                  boxSizing: 'border-box',
                }}
              />
            </div>
          </div>

          {/* ID Documents Section (Optional photos of front & back) */}
          <div style={{ border: '1px solid #e2e8f0', borderRadius: '12px', padding: '12px' }}>
            <button
              type="button"
              onClick={() => setShowIdDocs(!showIdDocs)}
              style={{
                width: '100%',
                background: 'none',
                border: 'none',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                padding: '2px',
                cursor: 'pointer',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                <CreditCard size={18} color="#1b4332" />
                <span style={{ fontSize: '13px', fontWeight: 800, color: '#1b4332' }}>
                  {isAr ? 'وثائق وبطاقة الرقم القومي (اختياري)' : 'National ID Card Photos (Optional)'}
                </span>
              </div>
              <span style={{ fontSize: '11px', color: '#64748b', fontWeight: 600 }}>
                {showIdDocs ? (isAr ? 'إخفاء' : 'Hide') : (isAr ? 'عرض وإضافة' : 'Show & Add')}
              </span>
            </button>

            {showIdDocs && (
              <div style={{ marginTop: '12px', display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
                {/* Front Photo */}
                <div style={{ border: '1px dashed #cbd5e1', borderRadius: '8px', padding: '8px', textAlign: 'center' }}>
                  <div style={{ fontSize: '11px', fontWeight: 700, color: '#475569', marginBottom: '6px' }}>
                    {isAr ? 'وجه البطاقة (الوجه الأمامي)' : 'ID Front Photo'}
                  </div>
                  {idCardPhotoFrontPath ? (
                    <div style={{ position: 'relative', width: '100%', height: '80px', marginBottom: '6px' }}>
                      <img
                        src={idCardPhotoFrontPath}
                        alt="ID Front"
                        style={{ width: '100%', height: '100%', objectFit: 'cover', borderRadius: '6px' }}
                      />
                      <button
                        type="button"
                        onClick={() => setIdCardPhotoFrontPath(undefined)}
                        style={{
                          position: 'absolute',
                          top: '2px',
                          right: '2px',
                          background: 'rgba(220,38,38,0.85)',
                          color: '#fff',
                          border: 'none',
                          borderRadius: '4px',
                          padding: '2px 4px',
                          cursor: 'pointer',
                        }}
                      >
                        <Trash2 size={12} />
                      </button>
                    </div>
                  ) : (
                    <div
                      style={{
                        height: '70px',
                        backgroundColor: '#f1f5f9',
                        borderRadius: '6px',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        marginBottom: '6px',
                      }}
                    >
                      <CreditCard size={28} color="#94a3b8" />
                    </div>
                  )}
                  <div style={{ display: 'flex', gap: '4px', justifyContent: 'center' }}>
                    <button
                      type="button"
                      onClick={() => handlePickPhoto('idFront', 'camera')}
                      style={{
                        padding: '4px 6px',
                        fontSize: '10px',
                        backgroundColor: '#1b4332',
                        color: '#fff',
                        border: 'none',
                        borderRadius: '4px',
                        cursor: 'pointer',
                      }}
                    >
                      {isAr ? 'كاميرا' : 'Camera'}
                    </button>
                    <button
                      type="button"
                      onClick={() => handlePickPhoto('idFront', 'photos')}
                      style={{
                        padding: '4px 6px',
                        fontSize: '10px',
                        backgroundColor: '#0284c7',
                        color: '#fff',
                        border: 'none',
                        borderRadius: '4px',
                        cursor: 'pointer',
                      }}
                    >
                      {isAr ? 'معرض' : 'Gallery'}
                    </button>
                  </div>
                </div>

                {/* Back Photo */}
                <div style={{ border: '1px dashed #cbd5e1', borderRadius: '8px', padding: '8px', textAlign: 'center' }}>
                  <div style={{ fontSize: '11px', fontWeight: 700, color: '#475569', marginBottom: '6px' }}>
                    {isAr ? 'ظهر البطاقة (الوجه الخلفي)' : 'ID Back Photo'}
                  </div>
                  {idCardPhotoBackPath ? (
                    <div style={{ position: 'relative', width: '100%', height: '80px', marginBottom: '6px' }}>
                      <img
                        src={idCardPhotoBackPath}
                        alt="ID Back"
                        style={{ width: '100%', height: '100%', objectFit: 'cover', borderRadius: '6px' }}
                      />
                      <button
                        type="button"
                        onClick={() => setIdCardPhotoBackPath(undefined)}
                        style={{
                          position: 'absolute',
                          top: '2px',
                          right: '2px',
                          background: 'rgba(220,38,38,0.85)',
                          color: '#fff',
                          border: 'none',
                          borderRadius: '4px',
                          padding: '2px 4px',
                          cursor: 'pointer',
                        }}
                      >
                        <Trash2 size={12} />
                      </button>
                    </div>
                  ) : (
                    <div
                      style={{
                        height: '70px',
                        backgroundColor: '#f1f5f9',
                        borderRadius: '6px',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        marginBottom: '6px',
                      }}
                    >
                      <CreditCard size={28} color="#94a3b8" />
                    </div>
                  )}
                  <div style={{ display: 'flex', gap: '4px', justifyContent: 'center' }}>
                    <button
                      type="button"
                      onClick={() => handlePickPhoto('idBack', 'camera')}
                      style={{
                        padding: '4px 6px',
                        fontSize: '10px',
                        backgroundColor: '#1b4332',
                        color: '#fff',
                        border: 'none',
                        borderRadius: '4px',
                        cursor: 'pointer',
                      }}
                    >
                      {isAr ? 'كاميرا' : 'Camera'}
                    </button>
                    <button
                      type="button"
                      onClick={() => handlePickPhoto('idBack', 'photos')}
                      style={{
                        padding: '4px 6px',
                        fontSize: '10px',
                        backgroundColor: '#0284c7',
                        color: '#fff',
                        border: 'none',
                        borderRadius: '4px',
                        cursor: 'pointer',
                      }}
                    >
                      {isAr ? 'معرض' : 'Gallery'}
                    </button>
                  </div>
                </div>
              </div>
            )}
          </div>

          {/* Notes */}
          <div>
            <label style={{ display: 'block', fontSize: '12px', fontWeight: 700, color: '#334155', marginBottom: '4px' }}>
              {isAr ? 'ملاحظات' : 'Notes'}
            </label>
            <textarea
              rows={2}
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              placeholder={isAr ? 'أي ملاحظات خاصة بالمستأجر...' : 'Any tenant notes...'}
              style={{
                width: '100%',
                padding: '8px 12px',
                borderRadius: '8px',
                border: '1.5px solid #cbd5e1',
                fontSize: '13px',
                boxSizing: 'border-box',
                resize: 'none',
              }}
            />
          </div>

          {/* Submit */}
          <div style={{ display: 'flex', gap: '10px', marginTop: '6px' }}>
            <button
              type="submit"
              style={{
                flex: 1,
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
              <Check size={18} />
              <span>{existingTenant ? (isAr ? 'حفظ التعديلات' : 'Save Changes') : (isAr ? 'إضافة المستأجر' : 'Add Tenant')}</span>
            </button>
            <button
              type="button"
              onClick={onClose}
              style={{
                padding: '13px 18px',
                borderRadius: '12px',
                border: '1px solid #cbd5e1',
                backgroundColor: '#f8fafc',
                color: '#475569',
                fontSize: '14px',
                fontWeight: 700,
                cursor: 'pointer',
              }}
            >
              {isAr ? 'إلغاء' : 'Cancel'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
