import React from 'react';
import { useApp } from '../context/AppContext';
import { Bell, Globe, Search, ShieldCheck } from 'lucide-react';

export const Header: React.FC = () => {
  const { language, toggleLanguage, activeTab, setActiveTab, notifications, searchQuery, setSearchQuery } = useApp();
  const [showSearch, setShowSearch] = React.useState(false);

  const unreadCount = notifications.length;

  return (
    <header
      style={{
        paddingTop: 'calc(var(--safe-top) + 12px)',
        paddingBottom: '12px',
        paddingLeft: '16px',
        paddingRight: '16px',
        backgroundColor: '#1b4332',
        color: '#ffffff',
        boxShadow: '0 2px 8px rgba(0,0,0,0.12)',
        position: 'sticky',
        top: 0,
        zIndex: 50,
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <div
            style={{
              width: '36px',
              height: '36px',
              borderRadius: '10px',
              backgroundColor: 'rgba(255,255,255,0.18)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <ShieldCheck size={22} color="#fcd34d" />
          </div>
          <div>
            <h1 style={{ fontSize: '18px', fontWeight: 800, margin: 0, lineHeight: 1.2 }}>
              {language === 'ar' ? 'إدارة الإيجارات' : 'Rent Manager'}
            </h1>
            <p style={{ fontSize: '11px', color: '#a7f3d0', margin: 0 }}>
              {language === 'ar' ? 'إدارة العقود والأملاك الخاصة' : 'Landlord Property Management'}
            </p>
          </div>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          {/* Search Toggle */}
          <button
            onClick={() => setShowSearch(!showSearch)}
            style={{
              width: '40px',
              height: '40px',
              borderRadius: '50%',
              backgroundColor: showSearch ? 'rgba(255,255,255,0.3)' : 'rgba(255,255,255,0.12)',
              border: 'none',
              color: '#ffffff',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              cursor: 'pointer',
            }}
            title={language === 'ar' ? 'بحث' : 'Search'}
            aria-label="Search"
          >
            <Search size={20} />
          </button>

          {/* Language Toggle */}
          <button
            onClick={toggleLanguage}
            style={{
              height: '40px',
              padding: '0 12px',
              borderRadius: '20px',
              backgroundColor: 'rgba(255,255,255,0.16)',
              border: '1px solid rgba(255,255,255,0.25)',
              color: '#ffffff',
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              fontSize: '13px',
              fontWeight: 700,
              cursor: 'pointer',
            }}
          >
            <Globe size={16} />
            <span>{language === 'ar' ? 'English' : 'عربي'}</span>
          </button>

          {/* Notification Bell */}
          <button
            onClick={() => setActiveTab('notifications')}
            style={{
              width: '40px',
              height: '40px',
              borderRadius: '50%',
              backgroundColor: activeTab === 'notifications' ? 'rgba(255,255,255,0.3)' : 'rgba(255,255,255,0.12)',
              border: 'none',
              color: '#ffffff',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              cursor: 'pointer',
              position: 'relative',
            }}
            title={language === 'ar' ? 'التنبيهات' : 'Notifications'}
            aria-label="Notifications"
          >
            <Bell size={20} />
            {unreadCount > 0 && (
              <span
                style={{
                  position: 'absolute',
                  top: '4px',
                  right: '4px',
                  backgroundColor: '#ef4444',
                  color: '#ffffff',
                  fontSize: '10px',
                  fontWeight: 800,
                  width: '18px',
                  height: '18px',
                  borderRadius: '50%',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  border: '2px solid #1b4332',
                }}
              >
                {unreadCount > 9 ? '9+' : unreadCount}
              </span>
            )}
          </button>
        </div>
      </div>

      {/* Expandable Search Bar */}
      {showSearch && (
        <div style={{ marginTop: '10px', display: 'flex', alignItems: 'center', gap: '8px' }}>
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder={
              language === 'ar'
                ? 'ابحث باسم المستأجر، رقم الوحدة، أو العقار...'
                : 'Search by tenant, unit number, or property...'
            }
            style={{
              flex: 1,
              height: '42px',
              padding: '0 14px',
              borderRadius: '8px',
              border: 'none',
              fontSize: '14px',
              outline: 'none',
              backgroundColor: '#ffffff',
              color: '#1e293b',
            }}
            autoFocus
          />
          {searchQuery && (
            <button
              onClick={() => setSearchQuery('')}
              style={{
                backgroundColor: 'rgba(255,255,255,0.2)',
                color: '#fff',
                border: 'none',
                borderRadius: '8px',
                height: '42px',
                padding: '0 12px',
                fontSize: '12px',
                fontWeight: 700,
                cursor: 'pointer',
              }}
            >
              {language === 'ar' ? 'مسح' : 'Clear'}
            </button>
          )}
        </div>
      )}
    </header>
  );
};
