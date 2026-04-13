document.addEventListener('DOMContentLoaded', () => {
	document.querySelectorAll('.admin-table-row-link[data-href]').forEach((row) => {
		row.addEventListener('click', () => {
			window.location.href = row.dataset.href;
		});

		row.addEventListener('keydown', (event) => {
			if (event.key === 'Enter' || event.key === ' ') {
				event.preventDefault();
				window.location.href = row.dataset.href;
			}
		});
	});

	document.querySelectorAll('table[data-sortable="true"]').forEach((table) => {
		const tbody = table.tBodies && table.tBodies[0];
		if (!tbody) return;

		const headers = Array.from(table.querySelectorAll('thead th'));
		headers.forEach((th, columnIndex) => {
			const label = (th.textContent || '').trim();
			if (!label) return;

			const sortType = th.dataset.sortType || 'text';
			th.innerHTML = `<button type="button" class="admin-sort-button" aria-sort="none">${label}<span class="admin-sort-icon">↕</span></button>`;
			const button = th.querySelector('.admin-sort-button');
			if (!button) return;

			button.addEventListener('click', (event) => {
				event.preventDefault();
				event.stopPropagation();

				const nextDirection = th.dataset.sortDirection === 'asc' ? 'desc' : 'asc';
				th.dataset.sortDirection = nextDirection;

				headers.forEach((header) => {
					if (header !== th) {
						header.dataset.sortDirection = '';
						const otherButton = header.querySelector('.admin-sort-button');
						const otherIcon = header.querySelector('.admin-sort-icon');
						if (otherButton) otherButton.setAttribute('aria-sort', 'none');
						if (otherIcon) otherIcon.textContent = '↕';
					}
				});

				button.setAttribute('aria-sort', nextDirection);
				const icon = th.querySelector('.admin-sort-icon');
				if (icon) icon.textContent = nextDirection === 'asc' ? '↑' : '↓';

				const rows = Array.from(tbody.querySelectorAll('tr'));
				rows.sort((a, b) => compareRows(a, b, columnIndex, sortType, nextDirection));
				rows.forEach((row) => tbody.appendChild(row));
			});
		});
	});

	initPermissionBatchApply();
});

function compareRows(rowA, rowB, columnIndex, sortType, direction) {
	const aCell = rowA.cells[columnIndex];
	const bCell = rowB.cells[columnIndex];
	const aRaw = aCell ? (aCell.textContent || '').trim() : '';
	const bRaw = bCell ? (bCell.textContent || '').trim() : '';

	const aValue = normalizeSortValue(aRaw, sortType);
	const bValue = normalizeSortValue(bRaw, sortType);

	if (aValue == null && bValue == null) return 0;
	if (aValue == null) return 1;
	if (bValue == null) return -1;

	let result = 0;
	if (typeof aValue === 'number' && typeof bValue === 'number') {
		result = aValue - bValue;
	} else {
		result = String(aValue).localeCompare(String(bValue), 'ja');
	}

	return direction === 'asc' ? result : -result;
}

function normalizeSortValue(raw, sortType) {
	if (!raw || raw === '-') return null;

	if (sortType === 'number') {
		const num = Number(raw.replace(/[^\d.-]/g, ''));
		return Number.isNaN(num) ? null : num;
	}

	if (sortType === 'date') {
		const normalized = raw.replace(/\./g, '/').replace(/-/g, '/');
		const time = new Date(normalized).getTime();
		return Number.isNaN(time) ? null : time;
	}

	return raw;
}

function initPermissionBatchApply() {
	const table = document.querySelector('table[data-permission-table="true"]');
	const applyButton = document.getElementById('permissions-apply-button');
	const payloadInput = document.getElementById('permissions-payload');
	const batchForm = document.getElementById('permissions-batch-form');
	if (!table || !applyButton || !payloadInput || !batchForm) return;

	const rows = Array.from(table.querySelectorAll('tbody tr[data-position]'));
	const snapshot = () => JSON.stringify(collectPermissions(rows));
	const initial = snapshot();

	const refreshApplyState = () => {
		const current = snapshot();
		applyButton.disabled = current === initial;
	};

	rows.forEach((row) => {
		row.querySelectorAll('input[type="checkbox"][data-perm-key]').forEach((checkbox) => {
			checkbox.addEventListener('change', () => {
				const key = checkbox.dataset.permKey;
				const checked = checkbox.checked;
				row.querySelectorAll(`input[type="checkbox"][data-perm-key="${key}"]`).forEach((peer) => {
					if (peer !== checkbox) peer.checked = checked;
				});
				refreshApplyState();
			});
		});
	});

	batchForm.addEventListener('submit', (event) => {
		const current = snapshot();
		if (current === initial) {
			event.preventDefault();
			applyButton.disabled = true;
			return;
		}
		payloadInput.value = current;
	});

	refreshApplyState();
}

function collectPermissions(rows) {
	return rows.map((row) => {
		const get = (key) => {
			const checkboxes = Array.from(row.querySelectorAll(`input[data-perm-key="${key}"]`));
			return checkboxes.some((checkbox) => checkbox.checked);
		};
		return {
			position: row.dataset.position || '',
			canDashboard: get('canDashboard'),
			canInquiries: get('canInquiries'),
			canMembers: get('canMembers'),
			canEmployees: get('canEmployees'),
			canShifts: get('canShifts'),
			canHamsters: get('canHamsters'),
			canProducts: get('canProducts'),
			canCafeCustomer: get('canCafeCustomer'),
			canCafe: get('canCafe')
		};
	});
}
