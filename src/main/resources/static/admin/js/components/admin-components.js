// admin-components.js - Các component tái sử dụng cho Admin Panel

/**
 * Data Table Component với tính năng tìm kiếm, phân trang, sắp xếp
 */
class AdminDataTable {
    constructor(containerId, options = {}) {
        this.container = document.getElementById(containerId);
        this.options = {
            columns: [],
            data: [],
            pageSize: 10,
            searchable: true,
            sortable: true,
            actions: [],
            ...options
        };
        this.currentPage = 1;
        this.sortColumn = null;
        this.sortDirection = 'asc';
        this.searchTerm = '';
        this.filteredData = [];

        this.init();
    }

    init() {
        this.render();
        this.bindEvents();
    }

    render() {
        this.container.innerHTML = `
            <div class="datatable-container">
                ${this.options.searchable ? this.renderSearch() : ''}
                <div class="datatable-wrapper">
                    ${this.renderTable()}
                </div>
                ${this.renderPagination()}
            </div>
        `;
    }

    renderSearch() {
        return `
            <div class="datatable-search">
                <input type="text" id="${this.container.id}-search" 
                       placeholder="Tìm kiếm..." class="search-input">
                <i class="fas fa-search"></i>
            </div>
        `;
    }

   renderTable() {
            let headers = this.options.columns.map(col => `
                <th ${this.options.sortable ? `class="sortable" data-column="${col.key}"` : ''}>
                    ${col.title}
                    ${this.options.sortable ? '<i class="fas fa-sort sort-icon"></i>' : ''}
                </th>
            `).join('');

            if (this.options.actions.length > 0) {
                headers += '<th>Thao tác</th>';
            }

        return `
            <table class="data-table">
                <thead>
                    <tr>${headers}</tr>
                </thead>
                <tbody id="${this.container.id}-tbody">
                    ${this.renderRows()}
                </tbody>
            </table>
        `;
    }

    renderRows() {
        if (this.getFilteredData().length === 0) {
            const colCount = this.options.columns.length + (this.options.actions.length > 0 ? 1 : 0);
            return `<tr><td colspan="${colCount}" class="no-data">Không có dữ liệu</td></tr>`;
        }

        const start = (this.currentPage - 1) * this.options.pageSize;
        const end = start + this.options.pageSize;
        const pageData = this.getFilteredData().slice(start, end);

        return pageData.map(row => {
            const cells = this.options.columns.map(col => {
                let value = this.getCellValue(row, col);
                if (col.render) {
                    value = col.render(value, row);
                }
                return `<td>${value}</td>`;
            }).join('');

            const actions = this.options.actions.length > 0 ? `
                <td>
                    <div class="action-buttons">
                        ${this.options.actions.map(action =>
                `<button class="btn btn-${action.type || 'outline'} action-btn" 
                                     onclick="${action.handler}(${row.id || row.userId})">
                                <i class="fas fa-${action.icon}"></i>
                                ${action.text || ''}
                             </button>`
            ).join('')}
                    </div>
                </td>
            ` : '';

            return `<tr>${cells}${actions}</tr>`;
        }).join('');
    }

    renderPagination() {
        const totalItems = this.getFilteredData().length;
        const totalPages = Math.ceil(totalItems / this.options.pageSize);

        if (totalPages <= 1) return '';

        const prevDisabled = this.currentPage === 1 ? 'disabled' : '';
        const nextDisabled = this.currentPage === totalPages ? 'disabled' : '';

        return `
            <div class="datatable-pagination">
                <div class="pagination-info">
                    Hiển thị ${(this.currentPage - 1) * this.options.pageSize + 1} - 
                    ${Math.min(this.currentPage * this.options.pageSize, totalItems)} 
                    trong ${totalItems} kết quả
                </div>
                <div class="pagination-controls">
                    <button class="btn btn-outline ${prevDisabled}" onclick="this.table.goToPage(${this.currentPage - 1})">
                        <i class="fas fa-chevron-left"></i>
                    </button>
                    ${this.renderPageNumbers(totalPages)}
                    <button class="btn btn-outline ${nextDisabled}" onclick="this.table.goToPage(${this.currentPage + 1})">
                        <i class="fas fa-chevron-right"></i>
                    </button>
                </div>
            </div>
        `;
    }

    renderPageNumbers(totalPages) {
        const pages = [];
        const start = Math.max(1, this.currentPage - 2);
        const end = Math.min(totalPages, this.currentPage + 2);

        for (let i = start; i <= end; i++) {
            const active = i === this.currentPage ? 'active' : '';
            pages.push(`
                <button class="btn btn-outline ${active}" onclick="this.table.goToPage(${i})">
                    ${i}
                </button>
            `);
        }

        return pages.join('');
    }

    bindEvents() {
        if (this.options.searchable) {
            const searchInput = document.getElementById(`${this.container.id}-search`);
            searchInput.addEventListener('input', this.debounce((e) => {
                this.searchTerm = e.target.value.toLowerCase();
                this.currentPage = 1;
                this.updateTable();
            }, 300));
        }

        if (this.options.sortable) {
            this.container.addEventListener('click', (e) => {
                if (e.target.classList.contains('sortable') || e.target.closest('.sortable')) {
                    const th = e.target.classList.contains('sortable') ? e.target : e.target.closest('.sortable');
                    this.handleSort(th.dataset.column);
                }
            });
        }

        // Store reference for pagination
        this.container.table = this;
    }

    handleSort(column) {
        if (this.sortColumn === column) {
            this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
        } else {
            this.sortColumn = column;
            this.sortDirection = 'asc';
        }

        this.updateTable();
        this.updateSortIcons();
    }

    updateSortIcons() {
        // Reset all sort icons
        this.container.querySelectorAll('.sort-icon').forEach(icon => {
            icon.className = 'fas fa-sort sort-icon';
        });

        // Update active sort icon
        const activeHeader = this.container.querySelector(`[data-column="${this.sortColumn}"]`);
        if (activeHeader) {
            const icon = activeHeader.querySelector('.sort-icon');
            icon.className = `fas fa-sort-${this.sortDirection === 'asc' ? 'up' : 'down'} sort-icon active`;
        }
    }

    getCellValue(row, column) {
        if (column.key.includes('.')) {
            const keys = column.key.split('.');
            let value = row;
            for (const key of keys) {
                value = value?.[key];
            }
            return value;
        }
        return row[column.key];
    }

    getFilteredData() {
        if (this.filteredData.length === 0 || this.searchTerm !== this.lastSearchTerm) {
            this.filteredData = this.filterData();
            this.lastSearchTerm = this.searchTerm;
        }
        return this.sortData(this.filteredData);
    }

    filterData() {
        if (!this.searchTerm) return this.options.data;

        return this.options.data.filter(row => {
            return this.options.columns.some(col => {
                const value = this.getCellValue(row, col);
                return String(value).toLowerCase().includes(this.searchTerm);
            });
        });
    }

    sortData(data) {
        if (!this.sortColumn) return data;

        const column = this.options.columns.find(col => col.key === this.sortColumn);
        if (!column) return data;

        return [...data].sort((a, b) => {
            let aVal = this.getCellValue(a, column);
            let bVal = this.getCellValue(b, column);

            // Handle different data types
            if (column.type === 'number') {
                aVal = Number(aVal) || 0;
                bVal = Number(bVal) || 0;
            } else if (column.type === 'date') {
                aVal = new Date(aVal);
                bVal = new Date(bVal);
            } else {
                aVal = String(aVal).toLowerCase();
                bVal = String(bVal).toLowerCase();
            }

            if (aVal < bVal) return this.sortDirection === 'asc' ? -1 : 1;
            if (aVal > bVal) return this.sortDirection === 'asc' ? 1 : -1;
            return 0;
        });
    }

    updateData(newData) {
        this.options.data = newData;
        this.filteredData = [];
        this.currentPage = 1;
        this.updateTable();
    }

    updateTable() {
        const tbody = document.getElementById(`${this.container.id}-tbody`);
        tbody.innerHTML = this.renderRows();

        const pagination = this.container.querySelector('.datatable-pagination');
        if (pagination) {
            pagination.outerHTML = this.renderPagination();
        }
    }

    goToPage(page) {
        const totalPages = Math.ceil(this.getFilteredData().length / this.options.pageSize);
        if (page >= 1 && page <= totalPages) {
            this.currentPage = page;
            this.updateTable();
        }
    }

    debounce(func, wait) {
        let timeout;
        return function executedFunction(...args) {
            const later = () => {
                clearTimeout(timeout);
                func(...args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    }
}

/**
 * Chart Component sử dụng Chart.js hoặc Canvas
 */
class AdminChart {
    constructor(canvasId, options = {}) {
        this.canvas = document.getElementById(canvasId);
        this.ctx = this.canvas.getContext('2d');
        this.options = {
            type: 'line',
            data: { labels: [], datasets: [] },
            ...options
        };

        this.init();
    }

    init() {
        this.render();
    }

    render() {
        // Simple chart implementation (you can replace with Chart.js)
        this.clearCanvas();

        switch (this.options.type) {
            case 'bar':
                this.renderBarChart();
                break;
            case 'pie':
                this.renderPieChart();
                break;
            case 'line':
            default:
                this.renderLineChart();
                break;
        }
    }

    clearCanvas() {
        this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);
    }

    renderLineChart() {
        const { labels, datasets } = this.options.data;
        if (!labels.length || !datasets.length) return;

        const padding = 50;
        const chartWidth = this.canvas.width - 2 * padding;
        const chartHeight = this.canvas.height - 2 * padding;

        // Draw axes
        this.ctx.strokeStyle = '#e5e7eb';
        this.ctx.lineWidth = 1;

        // Y-axis
        this.ctx.beginPath();
        this.ctx.moveTo(padding, padding);
        this.ctx.lineTo(padding, this.canvas.height - padding);
        this.ctx.stroke();

        // X-axis
        this.ctx.beginPath();
        this.ctx.moveTo(padding, this.canvas.height - padding);
        this.ctx.lineTo(this.canvas.width - padding, this.canvas.height - padding);
        this.ctx.stroke();

        // Draw data
        datasets.forEach((dataset, index) => {
            this.ctx.strokeStyle = dataset.borderColor || `hsl(${index * 60}, 70%, 50%)`;
            this.ctx.lineWidth = 2;
            this.ctx.beginPath();

            dataset.data.forEach((value, i) => {
                const x = padding + (i / (labels.length - 1)) * chartWidth;
                const y = this.canvas.height - padding - (value / Math.max(...dataset.data)) * chartHeight;

                if (i === 0) {
                    this.ctx.moveTo(x, y);
                } else {
                    this.ctx.lineTo(x, y);
                }
            });

            this.ctx.stroke();
        });

        // Draw labels
        this.ctx.fillStyle = '#6b7280';
        this.ctx.font = '12px sans-serif';
        this.ctx.textAlign = 'center';

        labels.forEach((label, i) => {
            const x = padding + (i / (labels.length - 1)) * chartWidth;
            this.ctx.fillText(label, x, this.canvas.height - padding + 20);
        });
    }

    renderBarChart() {
        // Simple bar chart implementation
        const { labels, datasets } = this.options.data;
        if (!labels.length || !datasets.length) return;

        const padding = 50;
        const chartWidth = this.canvas.width - 2 * padding;
        const chartHeight = this.canvas.height - 2 * padding;
        const barWidth = chartWidth / labels.length * 0.8;

        datasets.forEach((dataset, datasetIndex) => {
            dataset.data.forEach((value, i) => {
                const x = padding + (i * chartWidth / labels.length) + (chartWidth / labels.length - barWidth) / 2;
                const height = (value / Math.max(...dataset.data)) * chartHeight;
                const y = this.canvas.height - padding - height;

                this.ctx.fillStyle = dataset.backgroundColor || `hsl(${datasetIndex * 60}, 70%, 50%)`;
                this.ctx.fillRect(x, y, barWidth, height);
            });
        });

        // Draw labels
        this.ctx.fillStyle = '#6b7280';
        this.ctx.font = '12px sans-serif';
        this.ctx.textAlign = 'center';

        labels.forEach((label, i) => {
            const x = padding + (i * chartWidth / labels.length) + (chartWidth / labels.length) / 2;
            this.ctx.fillText(label, x, this.canvas.height - padding + 20);
        });
    }

    renderPieChart() {
        const { labels, datasets } = this.options.data;
        if (!labels.length || !datasets.length) return;

        const dataset = datasets[0];
        const total = dataset.data.reduce((sum, val) => sum + val, 0);
        const centerX = this.canvas.width / 2;
        const centerY = this.canvas.height / 2;
        const radius = Math.min(centerX, centerY) - 50;

        let currentAngle = -Math.PI / 2;

        dataset.data.forEach((value, i) => {
            const sliceAngle = (value / total) * 2 * Math.PI;

            this.ctx.fillStyle = `hsl(${i * 60}, 70%, 50%)`;
            this.ctx.beginPath();
            this.ctx.moveTo(centerX, centerY);
            this.ctx.arc(centerX, centerY, radius, currentAngle, currentAngle + sliceAngle);
            this.ctx.closePath();
            this.ctx.fill();

            currentAngle += sliceAngle;
        });
    }

    updateData(newData) {
        this.options.data = newData;
        this.render();
    }
}

/**
 * Form Builder Component
 */
class AdminFormBuilder {
    constructor(containerId, options = {}) {
        this.container = document.getElementById(containerId);
        this.options = {
            fields: [],
            submitText: 'Lưu',
            cancelText: 'Hủy',
            onSubmit: null,
            onCancel: null,
            ...options
        };

        this.init();
    }

    init() {
        this.render();
        this.bindEvents();
    }

    render() {
        this.container.innerHTML = `
            <form class="admin-form" id="${this.container.id}-form">
                ${this.renderFields()}
                <div class="form-actions">
                    <button type="button" class="btn btn-secondary cancel-btn">
                        ${this.options.cancelText}
                    </button>
                    <button type="submit" class="btn btn-primary submit-btn">
                        ${this.options.submitText}
                    </button>
                </div>
            </form>
        `;
    }

    renderFields() {
        return this.options.fields.map(field => {
            switch (field.type) {
                case 'select':
                    return this.renderSelect(field);
                case 'textarea':
                    return this.renderTextarea(field);
                case 'checkbox':
                    return this.renderCheckbox(field);
                case 'radio':
                    return this.renderRadio(field);
                case 'file':
                    return this.renderFile(field);
                default:
                    return this.renderInput(field);
            }
        }).join('');
    }

    renderInput(field) {
        return `
            <div class="form-group">
                <label for="${field.name}">${field.label}${field.required ? ' *' : ''}</label>
                <input type="${field.type || 'text'}" 
                       id="${field.name}" 
                       name="${field.name}"
                       value="${field.value || ''}"
                       placeholder="${field.placeholder || ''}"
                       ${field.required ? 'required' : ''}
                       ${field.readonly ? 'readonly' : ''}
                       ${field.disabled ? 'disabled' : ''}>
                ${field.help ? `<small class="form-help">${field.help}</small>` : ''}
            </div>
        `;
    }

    renderSelect(field) {
        const options = field.options.map(option =>
            `<option value="${option.value}" ${option.value === field.value ? 'selected' : ''}>
                ${option.label}
             </option>`
        ).join('');

        return `
            <div class="form-group">
                <label for="${field.name}">${field.label}${field.required ? ' *' : ''}</label>
                <select id="${field.name}" 
                        name="${field.name}"
                        ${field.required ? 'required' : ''}
                        ${field.disabled ? 'disabled' : ''}>
                    ${field.placeholder ? `<option value="">${field.placeholder}</option>` : ''}
                    ${options}
                </select>
                ${field.help ? `<small class="form-help">${field.help}</small>` : ''}
            </div>
        `;
    }

    renderTextarea(field) {
        return `
            <div class="form-group">
                <label for="${field.name}">${field.label}${field.required ? ' *' : ''}</label>
                <textarea id="${field.name}" 
                          name="${field.name}"
                          rows="${field.rows || 3}"
                          placeholder="${field.placeholder || ''}"
                          ${field.required ? 'required' : ''}
                          ${field.disabled ? 'disabled' : ''}>${field.value || ''}</textarea>
                ${field.help ? `<small class="form-help">${field.help}</small>` : ''}
            </div>
        `;
    }

    renderCheckbox(field) {
        return `
            <div class="form-group">
                <label class="checkbox-label">
                    <input type="checkbox" 
                           id="${field.name}" 
                           name="${field.name}"
                           value="${field.value || '1'}"
                           ${field.checked ? 'checked' : ''}
                           ${field.disabled ? 'disabled' : ''}>
                    ${field.label}
                </label>
                ${field.help ? `<small class="form-help">${field.help}</small>` : ''}
            </div>
        `;
    }

    renderRadio(field) {
        const radios = field.options.map(option => `
            <label class="radio-label">
                <input type="radio" 
                       name="${field.name}"
                       value="${option.value}"
                       ${option.value === field.value ? 'checked' : ''}
                       ${field.disabled ? 'disabled' : ''}>
                ${option.label}
            </label>
        `).join('');

        return `
            <div class="form-group">
                <fieldset>
                    <legend>${field.label}${field.required ? ' *' : ''}</legend>
                    <div class="radio-group">
                        ${radios}
                    </div>
                </fieldset>
                ${field.help ? `<small class="form-help">${field.help}</small>` : ''}
            </div>
        `;
    }

    renderFile(field) {
        return `
            <div class="form-group">
                <label for="${field.name}">${field.label}${field.required ? ' *' : ''}</label>
                <input type="file" 
                       id="${field.name}" 
                       name="${field.name}"
                       accept="${field.accept || ''}"
                       ${field.multiple ? 'multiple' : ''}
                       ${field.required ? 'required' : ''}
                       ${field.disabled ? 'disabled' : ''}>
                ${field.help ? `<small class="form-help">${field.help}</small>` : ''}
            </div>
        `;
    }

    bindEvents() {
        const form = this.container.querySelector('form');

        form.addEventListener('submit', (e) => {
            e.preventDefault();
            if (this.options.onSubmit) {
                const formData = this.getFormData();
                this.options.onSubmit(formData);
            }
        });

        const cancelBtn = this.container.querySelector('.cancel-btn');
        cancelBtn.addEventListener('click', () => {
            if (this.options.onCancel) {
                this.options.onCancel();
            }
        });
    }

    getFormData() {
        const form = this.container.querySelector('form');
        const formData = new FormData(form);
        const data = {};

        for (const [key, value] of formData.entries()) {
            if (data[key]) {
                // Handle multiple values (like checkboxes)
                if (!Array.isArray(data[key])) {
                    data[key] = [data[key]];
                }
                data[key].push(value);
            } else {
                data[key] = value;
            }
        }

        return data;
    }

    setFieldValue(fieldName, value) {
        const field = this.container.querySelector(`[name="${fieldName}"]`);
        if (field) {
            if (field.type === 'checkbox' || field.type === 'radio') {
                field.checked = field.value === value;
            } else {
                field.value = value;
            }
        }
    }

    getFieldValue(fieldName) {
        const field = this.container.querySelector(`[name="${fieldName}"]`);
        if (field) {
            if (field.type === 'checkbox') {
                return field.checked ? field.value : null;
            }
            return field.value;
        }
        return null;
    }

    validate() {
        const form = this.container.querySelector('form');
        return form.checkValidity();
    }

    reset() {
        const form = this.container.querySelector('form');
        form.reset();
    }
}

// Export components cho sử dụng global
window.AdminDataTable = AdminDataTable;
window.AdminChart = AdminChart;
window.AdminFormBuilder = AdminFormBuilder;