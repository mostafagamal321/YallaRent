import React, { useState } from 'react';
import { useApp } from '../context/AppContext';
import { formatUIDate } from '../utils/date';
import {
  User,
  Shield,
  Bell,
  FileCode,
  Download,
  Upload,
  RotateCcw,
  CheckCircle2,
  Lock,
  Globe,
  Save,
  AlertCircle,
} from 'lucide-react';

export const SettingsScreen: React.FC = () => {
  const {
    settings,
    updateSettings,
    resetTemplate,
    language,
    setLanguage,
    exportBackup,
    importBackup,
  } = useApp();

  const isAr = language === 'ar';

  // Landlord form state
  const [landlordName, setLandlordName] = useState(settings.landlord.name);
  const [landlordJob, setLandlordJob] = useState(settings.landlord.job);
  const [landlordAddress, setLandlordAddress] = useState(settings.landlord.address);
  const [landlordNationality, setLandlordNationality] = useState(settings.landlord.nationality);

  // PIN settings
  const [pinEnabled, setPinEnabled] = useState(settings.pinEnabled);
  const [pinCode, setPinCode] = useState(settings.pinCode);

  // Reminders
  const [rentDueDays, setRentDueDays] = useState(settings.reminders.rentDueDaysBefore);
  const [contractDays1, setContractDays1] = useState(settings.reminders.contractEndingDays1);
  const [contractDays2, setContractDays2] = useState(settings.reminders.contractEndingDays2);
  const [reminderTime, setReminderTime] = useState(settings.reminders.reminderTime);

  // Templates
  const [activeTemplateTab, setActiveTemplateTab] = useState<'apartments' | 'shops'>('apartments');
  const [apartmentTemplate, setApartmentTemplate] = useState(settings.templates.apartments);
  const [shopTemplate, setShopTemplate] = useState(settings.templates.shops);

  // Feedback states
  const [saveSuccess, setSaveSuccess] = useState(false);
  const [backupSuccess, setBackupSuccess] = useState(false);

  const handleSaveAllSettings = async () => {
    await updateSettings({
      pinEnabled,
      pinCode,
      landlord: {
        name: landlordName.trim(),
        job: landlordJob.trim(),
        address: landlordAddress.trim(),
        nationality: landlordNationality.trim(),
      },
      reminders: {
        ...settings.reminders,
        rentDueDaysBefore: Number(rentDueDays),
        contractEndingDays1: Number(contractDays1),
        contractEndingDays2: Number(contractDays2),
        reminderTime,
      },
      templates: {
        apartments: apartmentTemplate,
        shops: shopTemplate,
      },
    });

    setSaveSuccess(true);
    setTimeout(() => setSaveSuccess(false), 2500);
  };

  const handleResetTemplate = async (type: 'apartments' | 'shops') => {
    if (confirm(isAr ? 'هل أنت متأكد من استعادة النموذج الافتراضي؟' : 'Reset to default template?')) {
      await resetTemplate(type);
      if (type === 'apartments') setApartmentTemplate(settings.templates.apartments);
      else setShopTemplate(settings.templates.shops);
    }
  };

  const handleExportBackup = async () => {
    const success = await exportBackup();
    if (success) {
      setBackupSuccess(true);
      setTimeout(() => setBackupSuccess(false), 3000);
    }
  };

  const handleImportFile = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = async (event) => {
      const content = event.target?.result as string;
      if (confirm(isAr ? 'هل أنت متأكد من استعادة هذه النسخة الاحتياطية؟ سيتم تحديث كافة البيانات.' : 'Restore this backup? Current data will be replaced.')) {
        const ok = await importBackup(content);
        if (ok) {
          alert(isAr ? 'تم استرجاع البيانات بنجاح!' : 'Data restored successfully!');
        } else {
          alert(isAr ? 'حدث خطأ في قراءة ملف النسخة الاحتياطية.' : 'Invalid backup file.');
        }
      }
    };
    reader.readAsText(file);
  };

  return (
    <div style={{ padding: '16px', paddingBottom: '90px' }}>
      {/* Title */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
        <div>
          <h2 style={{ fontSize: '20px', fontWeight: 800, color: '#1e293b', margin: 0 }}>
            {isAr ? 'الإعدادات والبيانات' : 'Settings & Configurations'}
          </h2>
          <p style={{ fontSize: '13px', color: '#64748b', margin: '2px 0 0 0' }}>
            {isAr ? 'بيانات المالك، النماذج، الأمان والنسخ الاحتياطي' : 'Landlord info, templates, security & backup'}
          </p>
        </div>

        <button
          onClick={handleSaveAllSettings}
          style={{
            backgroundColor: '#1b4332',
            color: '#ffffff',
            border: 'none',
            borderRadius: '10px',
            padding: '10px 16px',
            fontSize: '13px',
            fontWeight: 700,
            display: 'flex',
            alignItems: 'center',
            gap: '6px',
            cursor: 'pointer',
          }}
        >
          <Save size={16} />
          <span>{isAr ? 'حفظ التعديلات' : 'Save Changes'}</span>
        </button>
      </div>

      {saveSuccess && (
        <div
          style={{
            backgroundColor: '#dcfce7',
            color: '#15803d',
            padding: '10px 14px',
            borderRadius: '10px',
            fontSize: '13px',
            fontWeight: 700,
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            marginBottom: '16px',
            border: '1px solid #bbf7d0',
          }}
        >
          <CheckCircle2 size={18} />
          <span>{isAr ? 'تم حفظ كافة الإعدادات بنجاح!' : 'All settings saved successfully!'}</span>
        </div>
      )}

      {/* 1. Landlord Details Section */}
      <div
        style={{
          backgroundColor: '#ffffff',
          borderRadius: '16px',
          padding: '18px',
          marginBottom: '18px',
          border: '1px solid #e2e8f0',
          boxShadow: '0 2px 6px rgba(0,0,0,0.03)',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '12px' }}>
          <User size={20} color="#1b4332" />
          <h3 style={{ fontSize: '16px', fontWeight: 800, color: '#1e293b', margin: 0 }}>
            {isAr ? 'بيانات المالك (تُستخدم في كافة العقود)' : 'Landlord Details'}
          </h3>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          <div>
            <label style={{ display: 'block', fontSize: '12px', fontWeight: 700, color: '#475569', marginBottom: '4px' }}>
              {isAr ? 'اسم المالك (المؤجر)' : 'Landlord Name'}
            </label>
            <input
              type="text"
              value={landlordName}
              onChange={(e) => setLandlordName(e.target.value)}
              style={{ width: '100%', height: '42px', padding: '0 12px', borderRadius: '8px', border: '1px solid #cbd5e1', fontSize: '14px', boxSizing: 'border-box' }}
            />
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
            <div>
              <label style={{ display: 'block', fontSize: '12px', fontWeight: 700, color: '#475569', marginBottom: '4px' }}>
                {isAr ? 'مهنة المالك' : 'Landlord Occupation'}
              </label>
              <input
                type="text"
                value={landlordJob}
                onChange={(e) => setLandlordJob(e.target.value)}
                style={{ width: '100%', height: '42px', padding: '0 10px', borderRadius: '8px', border: '1px solid #cbd5e1', fontSize: '13px', boxSizing: 'border-box' }}
              />
            </div>
            <div>
              <label style={{ display: 'block', fontSize: '12px', fontWeight: 700, color: '#475569', marginBottom: '4px' }}>
                {isAr ? 'جنسية المالك' : 'Nationality'}
              </label>
              <input
                type="text"
                value={landlordNationality}
                onChange={(e) => setLandlordNationality(e.target.value)}
                style={{ width: '100%', height: '42px', padding: '0 10px', borderRadius: '8px', border: '1px solid #cbd5e1', fontSize: '13px', boxSizing: 'border-box' }}
              />
            </div>
          </div>

          <div>
            <label style={{ display: 'block', fontSize: '12px', fontWeight: 700, color: '#475569', marginBottom: '4px' }}>
              {isAr ? 'عنوان وموطن المالك المختار' : 'Landlord Official Address'}
            </label>
            <textarea
              value={landlordAddress}
              onChange={(e) => setLandlordAddress(e.target.value)}
              rows={2}
              style={{ width: '100%', padding: '8px 12px', borderRadius: '8px', border: '1px solid #cbd5e1', fontSize: '13px', boxSizing: 'border-box', fontFamily: 'inherit' }}
            />
          </div>
        </div>
      </div>

      {/* 2. Security & PIN Lock */}
      <div
        style={{
          backgroundColor: '#ffffff',
          borderRadius: '16px',
          padding: '18px',
          marginBottom: '18px',
          border: '1px solid #e2e8f0',
          boxShadow: '0 2px 6px rgba(0,0,0,0.03)',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '12px' }}>
          <Lock size={20} color="#0284c7" />
          <h3 style={{ fontSize: '16px', fontWeight: 800, color: '#1e293b', margin: 0 }}>
            {isAr ? 'حماية التطبيق برمز مرور (PIN)' : 'Security & PIN Lock'}
          </h3>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '14px' }}>
          <div>
            <div style={{ fontSize: '14px', fontWeight: 700, color: '#334155' }}>
              {isAr ? 'تفعيل قفل التطبيق بـ PIN' : 'Enable App PIN Lock'}
            </div>
            <div style={{ fontSize: '12px', color: '#64748b' }}>
              {isAr ? 'طلب رمز المرور عند كل فتح للتطبيق' : 'Require 4-digit PIN upon opening the app'}
            </div>
          </div>
          <input
            type="checkbox"
            checked={pinEnabled}
            onChange={(e) => setPinEnabled(e.target.checked)}
            style={{ width: '22px', height: '22px', accentColor: '#1b4332', cursor: 'pointer' }}
          />
        </div>

        {pinEnabled && (
          <div>
            <label style={{ display: 'block', fontSize: '12px', fontWeight: 700, color: '#475569', marginBottom: '4px' }}>
              {isAr ? 'رمز المرور المكون من ٤ أرقام' : '4-Digit PIN Code'}
            </label>
            <input
              type="password"
              maxLength={4}
              value={pinCode}
              onChange={(e) => setPinCode(e.target.value.replace(/\D/g, ''))}
              placeholder="1234"
              style={{
                width: '140px',
                height: '42px',
                padding: '0 12px',
                borderRadius: '8px',
                border: '1px solid #cbd5e1',
                fontSize: '18px',
                letterSpacing: '4px',
                textAlign: 'center',
                boxSizing: 'border-box',
              }}
            />
          </div>
        )}
      </div>

      {/* 3. Notification Reminders Settings */}
      <div
        style={{
          backgroundColor: '#ffffff',
          borderRadius: '16px',
          padding: '18px',
          marginBottom: '18px',
          border: '1px solid #e2e8f0',
          boxShadow: '0 2px 6px rgba(0,0,0,0.03)',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '12px' }}>
          <Bell size={20} color="#d97706" />
          <h3 style={{ fontSize: '16px', fontWeight: 800, color: '#1e293b', margin: 0 }}>
            {isAr ? 'إعدادات مواعيد التنبيهات' : 'Reminder Timings'}
          </h3>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
            <div>
              <label style={{ display: 'block', fontSize: '12px', fontWeight: 700, color: '#475569', marginBottom: '4px' }}>
                {isAr ? 'تنبيه استحقاق الإيجار (قبل كم يوم)' : 'Rent due reminder (days before)'}
              </label>
              <input
                type="number"
                min={0}
                max={15}
                value={rentDueDays}
                onChange={(e) => setRentDueDays(Number(e.target.value))}
                style={{ width: '100%', height: '40px', padding: '0 10px', borderRadius: '8px', border: '1px solid #cbd5e1', fontSize: '13px', boxSizing: 'border-box' }}
              />
            </div>
            <div>
              <label style={{ display: 'block', fontSize: '12px', fontWeight: 700, color: '#475569', marginBottom: '4px' }}>
                {isAr ? 'وقت إرسال التنبيه اليومي' : 'Reminder Time'}
              </label>
              <input
                type="time"
                value={reminderTime}
                onChange={(e) => setReminderTime(e.target.value)}
                style={{ width: '100%', height: '40px', padding: '0 8px', borderRadius: '8px', border: '1px solid #cbd5e1', fontSize: '13px', boxSizing: 'border-box' }}
              />
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
            <div>
              <label style={{ display: 'block', fontSize: '12px', fontWeight: 700, color: '#475569', marginBottom: '4px' }}>
                {isAr ? 'تنبيه انتهاء العقد الأول (يوم)' : 'Contract ending (1st alert days)'}
              </label>
              <input
                type="number"
                value={contractDays1}
                onChange={(e) => setContractDays1(Number(e.target.value))}
                style={{ width: '100%', height: '40px', padding: '0 10px', borderRadius: '8px', border: '1px solid #cbd5e1', fontSize: '13px', boxSizing: 'border-box' }}
              />
            </div>
            <div>
              <label style={{ display: 'block', fontSize: '12px', fontWeight: 700, color: '#475569', marginBottom: '4px' }}>
                {isAr ? 'تنبيه انتهاء العقد الثاني (يوم)' : 'Contract ending (2nd alert days)'}
              </label>
              <input
                type="number"
                value={contractDays2}
                onChange={(e) => setContractDays2(Number(e.target.value))}
                style={{ width: '100%', height: '40px', padding: '0 10px', borderRadius: '8px', border: '1px solid #cbd5e1', fontSize: '13px', boxSizing: 'border-box' }}
              />
            </div>
          </div>
        </div>
      </div>

      {/* 4. Contract Templates Editor */}
      <div
        style={{
          backgroundColor: '#ffffff',
          borderRadius: '16px',
          padding: '18px',
          marginBottom: '18px',
          border: '1px solid #e2e8f0',
          boxShadow: '0 2px 6px rgba(0,0,0,0.03)',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '12px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <FileCode size={20} color="#15803d" />
            <h3 style={{ fontSize: '16px', fontWeight: 800, color: '#1e293b', margin: 0 }}>
              {isAr ? 'تعديل نماذج وبنود العقود' : 'Contract Templates'}
            </h3>
          </div>

          <button
            onClick={() => handleResetTemplate(activeTemplateTab)}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '4px',
              background: 'none',
              border: 'none',
              color: '#c2410c',
              fontSize: '12px',
              fontWeight: 700,
              cursor: 'pointer',
            }}
          >
            <RotateCcw size={14} />
            <span>{isAr ? 'استعادة الافتراضي' : 'Reset to Default'}</span>
          </button>
        </div>

        <div style={{ display: 'flex', gap: '10px', marginBottom: '10px' }}>
          <button
            onClick={() => setActiveTemplateTab('apartments')}
            style={{
              flex: 1,
              padding: '8px',
              borderRadius: '8px',
              border: activeTemplateTab === 'apartments' ? '2px solid #1b4332' : '1px solid #cbd5e1',
              backgroundColor: activeTemplateTab === 'apartments' ? '#dcfce7' : '#ffffff',
              color: activeTemplateTab === 'apartments' ? '#14532d' : '#475569',
              fontWeight: 700,
              cursor: 'pointer',
            }}
          >
            {isAr ? 'عقود الشقق السكنية' : 'Apartments Template'}
          </button>
          <button
            onClick={() => setActiveTemplateTab('shops')}
            style={{
              flex: 1,
              padding: '8px',
              borderRadius: '8px',
              border: activeTemplateTab === 'shops' ? '2px solid #1b4332' : '1px solid #cbd5e1',
              backgroundColor: activeTemplateTab === 'shops' ? '#fef3c7' : '#ffffff',
              color: activeTemplateTab === 'shops' ? '#92400e' : '#475569',
              fontWeight: 700,
              cursor: 'pointer',
            }}
          >
            {isAr ? 'عقود المحلات التجارية' : 'Shops Template'}
          </button>
        </div>

        <textarea
          value={activeTemplateTab === 'apartments' ? apartmentTemplate : shopTemplate}
          onChange={(e) => {
            if (activeTemplateTab === 'apartments') setApartmentTemplate(e.target.value);
            else setShopTemplate(e.target.value);
          }}
          rows={10}
          style={{
            width: '100%',
            padding: '10px',
            borderRadius: '8px',
            border: '1px solid #cbd5e1',
            fontSize: '12px',
            lineHeight: 1.5,
            fontFamily: 'inherit',
            boxSizing: 'border-box',
          }}
        />
      </div>

      {/* 5. Backup & Restore Section */}
      <div
        style={{
          backgroundColor: '#ffffff',
          borderRadius: '16px',
          padding: '18px',
          border: '1px solid #e2e8f0',
          boxShadow: '0 2px 6px rgba(0,0,0,0.03)',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '8px' }}>
          <Download size={20} color="#1b4332" />
          <h3 style={{ fontSize: '16px', fontWeight: 800, color: '#1e293b', margin: 0 }}>
            {isAr ? 'النسخ الاحتياطي واستعادة البيانات' : 'Backup & Restore'}
          </h3>
        </div>
        <p style={{ fontSize: '12px', color: '#64748b', marginBottom: '14px' }}>
          {isAr
            ? 'احفظ ملف نسخة احتياطية على هاتفك أو أرسله عبر واتساب/الإيميل لضمان عدم فقدان بياناتك.'
            : 'Save a backup JSON file on your device or share it to prevent data loss.'}
        </p>

        {settings.lastBackupDate && (
          <div style={{ fontSize: '12px', color: '#15803d', marginBottom: '14px', fontWeight: 700 }}>
            {isAr ? `تاريخ آخر نسخة احتياطية: ${formatUIDate(settings.lastBackupDate)}` : `Last backup: ${formatUIDate(settings.lastBackupDate)}`}
          </div>
        )}

        <div style={{ display: 'flex', gap: '10px' }}>
          <button
            onClick={handleExportBackup}
            style={{
              flex: 1,
              padding: '12px',
              borderRadius: '10px',
              backgroundColor: '#1b4332',
              color: '#ffffff',
              border: 'none',
              fontSize: '13px',
              fontWeight: 700,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '6px',
              cursor: 'pointer',
            }}
          >
            <Download size={16} />
            <span>{isAr ? 'تصدير نسخة احتياطية' : 'Export Backup'}</span>
          </button>

          <label
            style={{
              flex: 1,
              padding: '12px',
              borderRadius: '10px',
              backgroundColor: '#ffffff',
              color: '#1b4332',
              border: '1px solid #1b4332',
              fontSize: '13px',
              fontWeight: 700,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '6px',
              cursor: 'pointer',
            }}
          >
            <Upload size={16} />
            <span>{isAr ? 'استعادة نسخة' : 'Import Backup'}</span>
            <input
              type="file"
              accept=".json"
              onChange={handleImportFile}
              style={{ display: 'none' }}
            />
          </label>
        </div>
      </div>
    </div>
  );
};
