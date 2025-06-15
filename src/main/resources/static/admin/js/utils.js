// Utility functions
const { useState, useEffect, useCallback, useRef } = React;

const formatDate = (dateString) => {
    return new Date(dateString).toLocaleDateString('vi-VN');
};

const formatDateTime = (dateString) => {
    return new Date(dateString).toLocaleString('vi-VN');
};

// Export utilities
const exportUtils = {
    exportToPDF: (title, data) => {
        const { jsPDF } = window.jspdf;
        const doc = new jsPDF();

        // Add title
        doc.setFontSize(20);
        doc.text(title, 20, 20);

        // Add date
        doc.setFontSize(10);
        doc.text(`Xuất ngày: ${new Date().toLocaleDateString('vi-VN')}`, 20, 30);

        // Add data
        let yPosition = 50;
        doc.setFontSize(12);

        if (Array.isArray(data)) {
            data.forEach((item, index) => {
                if (yPosition > 270) {
                    doc.addPage();
                    yPosition = 20;
                }

                const text = typeof item === 'string' ? item : JSON.stringify(item, null, 2);
                const lines = doc.splitTextToSize(text, 170);
                doc.text(lines, 20, yPosition);
                yPosition += lines.length * 5 + 5;
            });
        }

        doc.save(`${title.replace(/\s+/g, '_')}_${new Date().getTime()}.pdf`);
    },

    exportToExcel: (title, data) => {
        const wb = XLSX.utils.book_new();

        // Convert data to worksheet format
        let wsData = [];
        if (Array.isArray(data) && data.length > 0) {
            if (typeof data[0] === 'object') {
                wsData = data;
            } else {
                wsData = data.map((item, index) => ({ 'STT': index + 1, 'Dữ liệu': item }));
            }
        }

        const ws = XLSX.utils.json_to_sheet(wsData);
        XLSX.utils.book_append_sheet(wb, ws, title);

        XLSX.writeFile(wb, `${title.replace(/\s+/g, '_')}_${new Date().getTime()}.xlsx`);
    }
};