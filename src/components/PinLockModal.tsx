import React, { useState } from 'react';
import { useApp } from '../context/AppContext';
import { Lock, Delete } from 'lucide-react';

export const PinLockModal: React.FC = () => {
  const { isPinLocked, unlockWithPin, language } = useApp();
  const [pin, setPin] = useState('');
  const [error, setError] = useState(false);

  if (!isPinLocked) return null;

  const handleKeyPress = (num: string) => {
    if (pin.length < 4) {
      const nextPin = pin + num;
      setPin(nextPin);
      setError(false);

      if (nextPin.length === 4) {
        const success = unlockWithPin(nextPin);
        if (!success) {
          setError(true);
          setTimeout(() => {
            setPin('');
          }, 400);
        }
      }
    }
  };

  const handleDelete = () => {
    setPin((prev) => prev.slice(0, -1));
    setError(false);
  };

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        backgroundColor: '#1b4332',
        color: '#ffffff',
        zIndex: 9999,
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '24px',
        paddingTop: 'calc(var(--safe-top) + 24px)',
        paddingBottom: 'calc(var(--safe-bottom) + 24px)',
      }}
    >
      <div
        style={{
          width: '72px',
          height: '72px',
          borderRadius: '50%',
          backgroundColor: 'rgba(255,255,255,0.15)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          marginBottom: '20px',
        }}
      >
        <Lock size={36} color="#fcd34d" />
      </div>

      <h2 style={{ fontSize: '22px', fontWeight: 800, marginBottom: '6px' }}>
        {language === 'ar' ? 'أهلاً بك في إدارة الإيجارات' : 'Welcome to Rent Manager'}
      </h2>
      <p style={{ fontSize: '14px', color: '#a7f3d0', marginBottom: '28px' }}>
        {language === 'ar' ? 'الرجاء إدخال رمز المرور (PIN) للمتابعة' : 'Please enter your PIN to continue'}
      </p>

      {/* PIN dots */}
      <div style={{ display: 'flex', gap: '16px', marginBottom: '36px' }}>
        {[0, 1, 2, 3].map((idx) => (
          <div
            key={idx}
            style={{
              width: '18px',
              height: '18px',
              borderRadius: '50%',
              backgroundColor: pin.length > idx ? (error ? '#ef4444' : '#fcd34d') : 'rgba(255,255,255,0.25)',
              border: '2px solid rgba(255,255,255,0.4)',
              transition: 'all 0.15s ease',
              transform: pin.length > idx ? 'scale(1.15)' : 'scale(1)',
            }}
          />
        ))}
      </div>

      {error && (
        <p style={{ color: '#fca5a5', fontSize: '13px', fontWeight: 700, marginBottom: '20px' }}>
          {language === 'ar' ? 'رمز المرور غير صحيح، حاول مرة أخرى' : 'Incorrect PIN, please try again'}
        </p>
      )}

      {/* Large Touch-friendly Keypad */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(3, 1fr)',
          gap: '16px',
          maxWidth: '280px',
          width: '100%',
        }}
      >
        {['1', '2', '3', '4', '5', '6', '7', '8', '9'].map((digit) => (
          <button
            key={digit}
            onClick={() => handleKeyPress(digit)}
            style={{
              width: '74px',
              height: '74px',
              borderRadius: '50%',
              backgroundColor: 'rgba(255,255,255,0.14)',
              border: '1px solid rgba(255,255,255,0.2)',
              color: '#ffffff',
              fontSize: '28px',
              fontWeight: 700,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              cursor: 'pointer',
              justifySelf: 'center',
              boxShadow: '0 2px 6px rgba(0,0,0,0.1)',
            }}
          >
            {digit}
          </button>
        ))}

        <div />
        <button
          onClick={() => handleKeyPress('0')}
          style={{
            width: '74px',
            height: '74px',
            borderRadius: '50%',
            backgroundColor: 'rgba(255,255,255,0.14)',
            border: '1px solid rgba(255,255,255,0.2)',
            color: '#ffffff',
            fontSize: '28px',
            fontWeight: 700,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            cursor: 'pointer',
            justifySelf: 'center',
          }}
        >
          0
        </button>
        <button
          onClick={handleDelete}
          style={{
            width: '74px',
            height: '74px',
            borderRadius: '50%',
            backgroundColor: 'rgba(255,255,255,0.1)',
            border: '1px solid rgba(255,255,255,0.2)',
            color: '#ffffff',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            cursor: 'pointer',
            justifySelf: 'center',
          }}
        >
          <Delete size={26} />
        </button>
      </div>
    </div>
  );
};
