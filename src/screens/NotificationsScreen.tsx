import React, { useState } from 'react';
import { useApp } from '../context/AppContext';
import {
  Bell,
  AlertTriangle,
  Clock,
  CalendarCheck,
  Database,
  ChevronLeft,
  ChevronRight,
  CheckCircle,
} from 'lucide-react';

interface NotificationsScreenProps {
  onOpenContractModal: (unitId?: string) => void;
  onOpenPaymentModal: (paymentId: string) => void;
  onOpenUnitDetail: (unitId: string) => void;
}

export const NotificationsScreen: React.FC<NotificationsScreenProps> = ({
  onOpenContractModal,
  onOpenPaymentModal,
  onOpenUnitDetail,
}) => {
  const { notifications, language, setActiveTab, payments } = useApp();
  const isAr = language === 'ar';
  const ChevronIcon = isAr ? ChevronLeft : ChevronRight;

  const [filterType, setFilterType] = useState<string>('all');

  const filtered = notifications.filter((n) => {
    if (filterType === 'all') return true;
    return n.type === filterType;
  });

  const getIcon = (type: string) => {
    switch (type) {
      case 'overdue':
        return <AlertTriangle size={20} color="#dc2626" />;
      case 'rent_due':
        return <Clock size={20} color="#d97706" />;
      case 'contract_expiring':
        return <CalendarCheck size={20} color="#0284c7" />;
      case 'backup_reminder':
        return <Database size={20} color="#16a34a" />;
      default:
        return <Bell size={20} color="#1b4332" />;
    }
  };

  const getBorderColor = (type: string) => {
    switch (type) {
      case 'overdue':
        return '#fca5a5';
      case 'rent_due':
        return '#fde68a';
      case 'contract_expiring':
        return '#bae6fd';
      default:
        return '#e2e8f0';
    }
  };

  const handleClickItem = (n: typeof notifications[0]) => {
    if (n.type === 'backup_reminder') {
      setActiveTab('settings');
    } else if (n.type === 'rent_due' || n.type === 'overdue') {
      // Find matching payment
      const payment = payments.find((p) => p.unitId === n.relatedUnitId && (p.status === 'pending' || p.status === 'overdue'));
      if (payment) {
        onOpenPaymentModal(payment.id);
      } else if (n.relatedUnitId) {
        onOpenUnitDetail(n.relatedUnitId);
      }
    } else if (n.type === 'contract_expiring') {
      if (n.relatedUnitId) {
        onOpenUnitDetail(n.relatedUnitId);
      }
    }
  };

  return (
    <div style={{ padding: '16px', paddingBottom: '90px' }}>
      {/* Title */}
      <div style={{ marginBottom: '14px' }}>
        <h2 style={{ fontSize: '20px', fontWeight: 800, color: '#1e293b', margin: 0 }}>
          {isAr ? 'مركز التنبيهات والإشعارات' : 'Notification Center'}
        </h2>
        <p style={{ fontSize: '13px', color: '#64748b', margin: '2px 0 0 0' }}>
          {isAr
            ? 'تنبيهات فورية بمواعيد الإيجارات وانتهاء العقود والتذكيرات'
            : 'Reminders for due rent, expiring leases, and backup'}
        </p>
      </div>

      {/* Filter Chips */}
      <div style={{ display: 'flex', gap: '8px', overflowX: 'auto', paddingBottom: '8px', marginBottom: '14px' }}>
        {[
          { id: 'all', labelAr: 'الكل', labelEn: 'All' },
          { id: 'overdue', labelAr: 'متأخرات', labelEn: 'Overdue' },
          { id: 'rent_due', labelAr: 'استحقاق إيجار', labelEn: 'Rent Due' },
          { id: 'contract_expiring', labelAr: 'انتهاء عقود', labelEn: 'Expiring Leases' },
          { id: 'backup_reminder', labelAr: 'نسخ احتياطي', labelEn: 'Backup' },
        ].map((chip) => (
          <button
            key={chip.id}
            onClick={() => setFilterType(chip.id)}
            style={{
              padding: '6px 14px',
              borderRadius: '20px',
              border: filterType === chip.id ? '1px solid #1b4332' : '1px solid #cbd5e1',
              backgroundColor: filterType === chip.id ? '#1b4332' : '#ffffff',
              color: filterType === chip.id ? '#ffffff' : '#475569',
              fontSize: '12px',
              fontWeight: 700,
              cursor: 'pointer',
              whiteSpace: 'nowrap',
            }}
          >
            {isAr ? chip.labelAr : chip.labelEn}
          </button>
        ))}
      </div>

      {/* Notifications List */}
      {filtered.length === 0 ? (
        <div
          style={{
            backgroundColor: '#ffffff',
            borderRadius: '16px',
            padding: '40px 16px',
            textAlign: 'center',
            color: '#64748b',
            border: '1px solid #e2e8f0',
          }}
        >
          <CheckCircle size={36} color="#16a34a" style={{ margin: '0 auto 10px auto' }} />
          <div style={{ fontSize: '15px', fontWeight: 800, color: '#1e293b', marginBottom: '4px' }}>
            {isAr ? 'لا توجد تنبيهات جديدة' : 'No new notifications'}
          </div>
          <div style={{ fontSize: '13px' }}>
            {isAr ? 'جميع المستحقات والعقود تحت المتابعة المنتظمة.' : 'All payments and leases are up to date.'}
          </div>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
          {filtered.map((item) => (
            <div
              key={item.id}
              onClick={() => handleClickItem(item)}
              style={{
                backgroundColor: '#ffffff',
                borderRadius: '14px',
                padding: '14px 16px',
                border: `1.5px solid ${getBorderColor(item.type)}`,
                display: 'flex',
                alignItems: 'center',
                gap: '12px',
                boxShadow: '0 2px 6px rgba(0,0,0,0.03)',
                cursor: 'pointer',
              }}
            >
              <div
                style={{
                  width: '40px',
                  height: '40px',
                  borderRadius: '10px',
                  backgroundColor: '#f8fafc',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  flexShrink: 0,
                }}
              >
                {getIcon(item.type)}
              </div>

              <div style={{ flex: 1 }}>
                <h4 style={{ fontSize: '14px', fontWeight: 800, color: '#0f172a', margin: '0 0 2px 0' }}>
                  {item.title}
                </h4>
                <p style={{ fontSize: '12px', color: '#475569', margin: 0, lineHeight: 1.4 }}>
                  {item.message}
                </p>
              </div>

              <ChevronIcon size={18} color="#94a3b8" />
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
