import React, { useState, useMemo } from 'react';
import { useApp } from '../context/AppContext';
import { formatCurrency } from '../utils/number';
import { toISODate, formatUIDate } from '../utils/date';
import jsPDF from 'jspdf';
import html2canvas from 'html2canvas';
import { Filesystem, Directory } from '@capacitor/filesystem';
import { Share } from '@capacitor/share';
import {
  FileSpreadsheet,
  Download,
  Share2,
  Calendar,
  Building,
  CheckCircle2,
  AlertTriangle,
  ArrowRight,
  ArrowLeft,
} from 'lucide-react';

export const ReportsScreen: React.FC = () => {
  const { properties, units, contracts, payments, language, settings, setActiveTab } = useApp();
  const isAr = language === 'ar';
  const BackIcon = isAr ? ArrowRight : ArrowLeft;

  const [selectedMonth, setSelectedMonth] = useState<string>(toISODate(new Date()).slice(0, 7)); // YYYY-MM
  const [isExporting, setIsExporting] = useState(false);

  // Available months
  const availableMonths = useMemo(() => {
    const set = new Set<string>();
    for (const p of payments) {
      set.add(p.dueDate.slice(0, 7));
    }
    const current = toISODate(new Date()).slice(0, 7);
    set.add(current);
    return Array.from(set).sort().reverse();
  }, [payments]);

  // Aggregate monthly report data per property
  const propertyReports = useMemo(() => {
    return properties.map((prop) => {
      const propUnits = units.filter((u) => u.propertyId === prop.id);
      const propUnitIds = new Set(propUnits.map((u) => u.id));

      const propPayments = payments.filter(
        (p) => propUnitIds.has(p.unitId) && p.dueDate.startsWith(selectedMonth)
      );

      const expected = propPayments.reduce((sum, p) => sum + p.amount, 0);
      const collected = propPayments
        .filter((p) => p.status === 'paid')
        .reduce((sum, p) => sum + (p.paidAmount || p.amount), 0);
      const overdue = propPayments
        .filter((p) => p.status === 'overdue')
        .reduce((sum, p) => sum + (p.amount - (p.paidAmount || 0)), 0);

      const activeLeases = contracts.filter(
        (c) => !c.isHistory && propUnitIds.has(c.unitId) && c.status === 'active'
      ).length;

      const rate = expected > 0 ? Math.round((collected / expected) * 100) : 0;

      return {
        property: prop,
        totalUnits: propUnits.length,
        activeLeases,
        expected,
        collected,
        overdue,
        rate,
      };
    });
  }, [properties, units, contracts, payments, selectedMonth]);

  const grandTotalExpected = propertyReports.reduce((sum, r) => sum + r.expected, 0);
  const grandTotalCollected = propertyReports.reduce((sum, r) => sum + r.collected, 0);
  const grandTotalOverdue = propertyReports.reduce((sum, r) => sum + r.overdue, 0);
  const overallRate = grandTotalExpected > 0 ? Math.round((grandTotalCollected / grandTotalExpected) * 100) : 0;

  // Export PDF Report
  const handleExportPDF = async () => {
    setIsExporting(true);
    const reportContainer = document.getElementById('report-printable-area');
    if (!reportContainer) {
      setIsExporting(false);
      return;
    }

    try {
      const canvas = await html2canvas(reportContainer, {
        scale: 2,
        useCORS: true,
        backgroundColor: '#ffffff',
      });

      const pdf = new jsPDF({
        orientation: 'portrait',
        unit: 'pt',
        format: 'a4',
      });

      const pdfWidth = pdf.internal.pageSize.getWidth();
      const pdfHeight = (canvas.height * pdfWidth) / canvas.width;
      const imgData = canvas.toDataURL('image/jpeg', 0.95);

      pdf.addImage(imgData, 'JPEG', 0, 0, pdfWidth, pdfHeight);

      const fileName = `تقرير_التحصيل_الشهري_${selectedMonth}.pdf`;

      try {
        const base64 = pdf.output('datauristring').split(',')[1];
        const writeRes = await Filesystem.writeFile({
          path: fileName,
          data: base64,
          directory: Directory.Cache,
        });

        await Share.share({
          title: `تقرير تحصيل الإيجارات - شهر ${selectedMonth}`,
          text: `ملخص تحصيل إيجارات العقارات لشهر ${selectedMonth}`,
          url: writeRes.uri,
          dialogTitle: 'مشاركة تقرير التحصيل',
        });
      } catch {
        pdf.save(fileName);
      }
    } catch (e) {
      console.error('Report PDF generation error:', e);
    } finally {
      setIsExporting(false);
    }
  };

  return (
    <div style={{ padding: '16px', paddingBottom: '90px' }}>
      {/* Top Bar */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '14px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <button
            onClick={() => setActiveTab('dashboard')}
            style={{
              background: 'none',
              border: 'none',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              padding: '4px',
            }}
          >
            <BackIcon size={22} color="#1b4332" />
          </button>
          <div>
            <h2 style={{ fontSize: '20px', fontWeight: 800, color: '#1e293b', margin: 0 }}>
              {isAr ? 'تقارير التحصيل الشهرية' : 'Monthly Collection Reports'}
            </h2>
            <p style={{ fontSize: '12px', color: '#64748b', margin: '2px 0 0 0' }}>
              {isAr ? 'ملخص الإيرادات والمتأخرات لكل عقار' : 'Monthly revenue and collection per property'}
            </p>
          </div>
        </div>

        <button
          onClick={handleExportPDF}
          disabled={isExporting}
          style={{
            backgroundColor: '#1b4332',
            color: '#ffffff',
            border: 'none',
            borderRadius: '10px',
            padding: '10px 14px',
            fontSize: '12px',
            fontWeight: 700,
            display: 'flex',
            alignItems: 'center',
            gap: '6px',
            cursor: isExporting ? 'wait' : 'pointer',
          }}
        >
          <Share2 size={16} />
          <span>{isExporting ? (isAr ? 'جارِ التصدير...' : 'Exporting...') : isAr ? 'تصدير PDF' : 'Export PDF'}</span>
        </button>
      </div>

      {/* Month Selector */}
      <div
        style={{
          backgroundColor: '#ffffff',
          borderRadius: '12px',
          padding: '12px 16px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          border: '1px solid #cbd5e1',
          marginBottom: '16px',
        }}
      >
        <span style={{ fontSize: '13px', fontWeight: 700, color: '#334155' }}>
          {isAr ? 'اختر شهر التقرير:' : 'Select Month:'}
        </span>
        <select
          value={selectedMonth}
          onChange={(e) => setSelectedMonth(e.target.value)}
          style={{
            padding: '8px 12px',
            borderRadius: '8px',
            border: '1px solid #cbd5e1',
            fontSize: '13px',
            fontWeight: 700,
            backgroundColor: '#f8fafc',
            color: '#1b4332',
          }}
        >
          {availableMonths.map((m) => (
            <option key={m} value={m}>{m}</option>
          ))}
        </select>
      </div>

      {/* Printable Area for PDF capture */}
      <div id="report-printable-area" style={{ backgroundColor: '#ffffff', borderRadius: '16px', padding: '16px', border: '1px solid #e2e8f0', boxShadow: '0 2px 8px rgba(0,0,0,0.04)' }}>
        <div style={{ textAlign: 'center', borderBottom: '2px solid #1b4332', paddingBottom: '12px', marginBottom: '16px' }}>
          <h3 style={{ fontSize: '18px', fontWeight: 800, color: '#1b4332', margin: '0 0 4px 0' }}>
            {isAr ? 'تقرير تحصيل الإيجارات الشهري' : 'Monthly Rent Collection Report'}
          </h3>
          <p style={{ fontSize: '12px', color: '#64748b', margin: 0 }}>
            {isAr ? `عن شهر: ${selectedMonth} • المالك: ${settings.landlord.name}` : `For month: ${selectedMonth} • Landlord: ${settings.landlord.name}`}
          </p>
        </div>

        {/* Overall Progress Banner */}
        <div style={{ backgroundColor: '#f0fdf4', borderRadius: '12px', padding: '14px', border: '1px solid #bbf7d0', marginBottom: '16px' }}>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '8px', textAlign: 'center' }}>
            <div>
              <div style={{ fontSize: '11px', color: '#166534' }}>{isAr ? 'المطلوب' : 'Expected'}</div>
              <div style={{ fontSize: '15px', fontWeight: 800, color: '#14532d', marginTop: '2px' }}>
                {formatCurrency(grandTotalExpected, language)}
              </div>
            </div>
            <div>
              <div style={{ fontSize: '11px', color: '#166534' }}>{isAr ? 'المحصل' : 'Collected'}</div>
              <div style={{ fontSize: '15px', fontWeight: 800, color: '#15803d', marginTop: '2px' }}>
                {formatCurrency(grandTotalCollected, language)}
              </div>
            </div>
            <div>
              <div style={{ fontSize: '11px', color: '#991b1b' }}>{isAr ? 'المتأخرات' : 'Overdue'}</div>
              <div style={{ fontSize: '15px', fontWeight: 800, color: '#dc2626', marginTop: '2px' }}>
                {formatCurrency(grandTotalOverdue, language)}
              </div>
            </div>
            <div>
              <div style={{ fontSize: '11px', color: '#1e40af' }}>{isAr ? 'نسبة التحصيل' : 'Rate'}</div>
              <div style={{ fontSize: '16px', fontWeight: 800, color: '#2563eb', marginTop: '2px' }}>
                {overallRate}%
              </div>
            </div>
          </div>
        </div>

        {/* Breakdown by Property */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          {propertyReports.map((r) => (
            <div
              key={r.property.id}
              style={{
                border: '1px solid #cbd5e1',
                borderRadius: '12px',
                padding: '12px 14px',
                backgroundColor: '#ffffff',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '8px' }}>
                <div>
                  <h4 style={{ fontSize: '15px', fontWeight: 800, color: '#1e293b', margin: 0 }}>
                    {r.property.name}
                  </h4>
                  <span style={{ fontSize: '11px', color: '#64748b' }}>
                    {r.property.type === 'shops' ? (isAr ? 'محلات تجارية' : 'Shops') : (isAr ? 'عمارة سكنية' : 'Apartments')} • {r.activeLeases} من {r.totalUnits} {isAr ? 'مؤجرة' : 'leased'}
                  </span>
                </div>
                <span
                  style={{
                    backgroundColor: r.rate >= 90 ? '#dcfce7' : r.rate >= 50 ? '#fef3c7' : '#fee2e2',
                    color: r.rate >= 90 ? '#15803d' : r.rate >= 50 ? '#b45309' : '#b91c1c',
                    fontSize: '12px',
                    fontWeight: 800,
                    padding: '3px 8px',
                    borderRadius: '6px',
                  }}
                >
                  {r.rate}%
                </span>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '8px', fontSize: '12px', backgroundColor: '#f8fafc', padding: '8px', borderRadius: '8px' }}>
                <div>
                  <span style={{ color: '#64748b', display: 'block' }}>{isAr ? 'المطلوب:' : 'Due:'}</span>
                  <strong>{formatCurrency(r.expected, language)}</strong>
                </div>
                <div>
                  <span style={{ color: '#15803d', display: 'block' }}>{isAr ? 'المحصل:' : 'Paid:'}</span>
                  <strong style={{ color: '#15803d' }}>{formatCurrency(r.collected, language)}</strong>
                </div>
                <div>
                  <span style={{ color: '#dc2626', display: 'block' }}>{isAr ? 'المتبقي:' : 'Left:'}</span>
                  <strong style={{ color: '#dc2626' }}>{formatCurrency(r.overdue, language)}</strong>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};
