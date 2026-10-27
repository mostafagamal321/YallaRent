import React from 'react';
import { useApp, NavigationTab } from '../context/AppContext';
import { LayoutDashboard, Building2, Users, ReceiptText, BellRing, Settings, LucideIcon } from 'lucide-react';

export const BottomNav: React.FC = () => {
  const { activeTab, setActiveTab, language, notifications } = useApp();

  const tabs: { id: NavigationTab; labelAr: string; labelEn: string; icon: LucideIcon }[] = [
    { id: 'dashboard', labelAr: 'الرئيسية', labelEn: 'Dashboard', icon: LayoutDashboard },
    { id: 'properties', labelAr: 'العقارات', labelEn: 'Properties', icon: Building2 },
    { id: 'tenants', labelAr: 'المستأجرون', labelEn: 'Tenants', icon: Users },
    { id: 'payments', labelAr: 'التحصيل', labelEn: 'Payments', icon: ReceiptText },
    { id: 'notifications', labelAr: 'التنبيهات', labelEn: 'Alerts', icon: BellRing },
    { id: 'settings', labelAr: 'الإعدادات', labelEn: 'Settings', icon: Settings },
  ];

  const unreadCount = notifications.length;

  return (
    <nav
      style={{
        position: 'fixed',
        bottom: 0,
        left: 0,
        right: 0,
        backgroundColor: '#ffffff',
        borderTop: '1px solid #e2e8f0',
        paddingBottom: 'calc(var(--safe-bottom) + 6px)',
        paddingTop: '6px',
        display: 'flex',
        justifyContent: 'space-around',
        alignItems: 'center',
        zIndex: 40,
        boxShadow: '0 -2px 10px rgba(0,0,0,0.06)',
      }}
    >
      {tabs.map((tab) => {
        const IconComponent = tab.icon;
        const isActive = activeTab === tab.id;

        return (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id)}
            style={{
              flex: 1,
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              justifyContent: 'center',
              padding: '6px 2px',
              minHeight: '48px',
              background: 'none',
              border: 'none',
              color: isActive ? '#1b4332' : '#64748b',
              cursor: 'pointer',
              position: 'relative',
              transition: 'all 0.15s ease',
            }}
          >
            <div
              style={{
                width: '40px',
                height: '28px',
                borderRadius: '14px',
                backgroundColor: isActive ? '#d8f3dc' : 'transparent',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                position: 'relative',
                marginBottom: '3px',
              }}
            >
              <IconComponent size={20} />
              {tab.id === 'notifications' && unreadCount > 0 && (
                <span
                  style={{
                    position: 'absolute',
                    top: '-2px',
                    right: '4px',
                    width: '8px',
                    height: '8px',
                    borderRadius: '50%',
                    backgroundColor: '#ef4444',
                  }}
                />
              )}
            </div>
            <span
              style={{
                fontSize: '11px',
                fontWeight: isActive ? 800 : 500,
                lineHeight: 1,
              }}
            >
              {language === 'ar' ? tab.labelAr : tab.labelEn}
            </span>
          </button>
        );
      })}
    </nav>
  );
};
