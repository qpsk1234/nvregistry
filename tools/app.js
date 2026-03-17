const PAGE_SIZE = 100;

let dataA = null;
let dataB = null;
let currentMode = 'normal'; // 'normal', 'compare'
let comparisonResult = [];
let filteredData = [];
let currentIndex = 0;

// DOM Elements
const statusBar = document.getElementById('statusBar');
const fileInput1 = document.getElementById('fileInput1');
const fileInput2 = document.getElementById('fileInput2');
const fileInput2Group = document.getElementById('fileInput2Group');
const searchInput = document.getElementById('searchInput');
const diffOnlyCheckbox = document.getElementById('diffOnlyCheckbox');
const diffFilterContainer = document.getElementById('diffFilterContainer');
const registryBody = document.getElementById('registryBody');
const tableHeader = document.getElementById('tableHeader');
const resultsCount = document.getElementById('resultsCount');
const loadMoreBtn = document.getElementById('loadMoreBtn');
const loadMoreContainer = document.getElementById('loadMoreContainer');

// Mode & Theme Buttons
const modeNormalBtn = document.getElementById('modeNormalBtn');
const modeCompareBtn = document.getElementById('modeCompareBtn');
const themeBlackBtn = document.getElementById('themeBlackBtn');
const themeWhiteBtn = document.getElementById('themeWhiteBtn');

// Conversion Logic
function parsePayload(payload, typeName, size) {
    if (!payload) return '';
    const bytes = payload.split(',').map(h => parseInt(h, 16));

    const results = [];
    for (let i = 0; i < bytes.length; i += size) {
        let val = 0n;
        for (let j = 0; j < size; j++) {
            if (i + j < bytes.length) {
                val |= BigInt(bytes[i + j]) << BigInt(j * 8);
            }
        }

        if (typeName.startsWith('s')) {
            const bitSize = size * 8;
            const limit = 1n << BigInt(bitSize - 1);
            if (val >= limit) {
                val -= 1n << BigInt(bitSize);
            }
        }
        results.push(val.toString());
    }

    return results.join(', ');
}

function updateTableHeader() {
    if (currentMode === 'normal') {
        tableHeader.innerHTML = `
            <tr>
                <th class="col-index">Idx</th>
                <th class="col-name">RegistryName</th>
                <th class="col-type">Type</th>
                <th class="col-payload">Payload (Hex)</th>
                <th class="col-value">Value (Dec)</th>
            </tr>
        `;
        document.getElementById('registryTable').classList.remove('compare-mode');
    } else {
        tableHeader.innerHTML = `
            <tr>
                <th class="col-index">Idx</th>
                <th class="col-name">RegistryName</th>
                <th class="col-type">Type</th>
                <th class="col-payload">File A (Hex/Dec)</th>
                <th class="col-value">File B (Hex/Dec)</th>
            </tr>
        `;
        document.getElementById('registryTable').classList.add('compare-mode');
    }
}

function compareData() {
    if (!dataA) return;

    const mapA = new Map(dataA.map(item => [item.RegistryName, item]));
    const mapB = dataB ? new Map(dataB.map(item => [item.RegistryName, item])) : new Map();

    const allKeys = new Set([...mapA.keys(), ...mapB.keys()]);

    comparisonResult = Array.from(allKeys).map(key => {
        const itemA = mapA.get(key);
        const itemB = mapB.get(key);

        let diffType = 'none';
        if (!itemA) diffType = 'added';
        else if (!itemB) diffType = 'removed';
        else if (itemA.Payload !== itemB.Payload) diffType = 'changed';

        const baseItem = itemB || itemA;
        return {
            ...baseItem,
            diffType,
            itemA,
            itemB
        };
    });

    comparisonResult.sort((a, b) => (a.Index || 0) - (b.Index || 0));
    applyFilter();
}

function renderNextBatch() {
    const end = Math.min(currentIndex + PAGE_SIZE, filteredData.length);
    const fragment = document.createDocumentFragment();

    for (let i = currentIndex; i < end; i++) {
        const item = filteredData[i];
        const tr = document.createElement('tr');

        if (currentMode === 'normal') {
            tr.innerHTML = `
                <td>${item.Index ?? '-'}</td>
                <td style="font-weight: 500;">${item.RegistryName}</td>
                <td class="mono">${item.TypeName} (${item.Size})</td>
                <td class="mono"><span class="payload-text">${item.Payload}</span></td>
                <td class="mono" style="color: #fbbf24;">${parsePayload(item.Payload, item.TypeName, item.Size)}</td>
            `;
        } else {
            // Compare Mode
            if (item.diffType !== 'none') tr.classList.add(`diff-${item.diffType}`);

            let tagHtml = '';
            if (item.diffType === 'added') tagHtml = '<span class="tag tag-added">ADD</span>';
            if (item.diffType === 'removed') tagHtml = '<span class="tag tag-removed">DEL</span>';
            if (item.diffType === 'changed') tagHtml = '<span class="tag tag-changed">CHG</span>';

            const renderVal = (entry) => {
                if (!entry) return '<span style="opacity: 0.3;">-</span>';
                const dec = parsePayload(entry.Payload, entry.TypeName, entry.Size);
                return `
                    <div class="mono payload-text">${entry.Payload}</div>
                    <div class="mono" style="color: #fbbf24; font-size: 0.7rem;">${dec}</div>
                `;
            };

            tr.innerHTML = `
                <td>${item.Index ?? '-'}</td>
                <td style="font-weight: 500;">${tagHtml}${item.RegistryName}</td>
                <td class="mono">${item.TypeName} (${item.Size})</td>
                <td>${renderVal(item.itemA)}</td>
                <td>${renderVal(item.itemB)}</td>
            `;
        }
        fragment.appendChild(tr);
    }

    registryBody.appendChild(fragment);
    currentIndex = end;
    loadMoreContainer.style.display = currentIndex < filteredData.length ? 'block' : 'none';
}

function applyFilter() {
    const query = searchInput.value.toLowerCase();
    const diffOnly = diffOnlyCheckbox.checked;

    const source = (currentMode === 'compare') ? comparisonResult : (dataA || []);

    filteredData = source.filter(item => {
        const name = item.RegistryName || '';
        const matchesQuery = name.toLowerCase().includes(query);
        const matchesDiff = !diffOnly || (currentMode === 'normal') || item.diffType !== 'none';
        return matchesQuery && matchesDiff;
    });

    registryBody.innerHTML = '';
    currentIndex = 0;
    resultsCount.textContent = `${filteredData.length} 件見つかりました`;
    renderNextBatch();
}

// Event Listeners
searchInput.addEventListener('input', applyFilter);
diffOnlyCheckbox.addEventListener('change', applyFilter);
loadMoreBtn.addEventListener('click', renderNextBatch);

modeNormalBtn.addEventListener('click', () => {
    currentMode = 'normal';
    modeNormalBtn.classList.add('active');
    modeCompareBtn.classList.remove('active');
    fileInput2Group.style.display = 'none';
    diffFilterContainer.style.display = 'none';
    updateTableHeader();
    applyFilter();
});

modeCompareBtn.addEventListener('click', () => {
    currentMode = 'compare';
    modeCompareBtn.classList.add('active');
    modeNormalBtn.classList.remove('active');
    fileInput2Group.style.display = 'flex';
    diffFilterContainer.style.display = 'block';
    updateTableHeader();
    compareData(); // This will call applyFilter
});

themeBlackBtn.addEventListener('click', () => {
    document.body.classList.remove('light-theme');
    themeBlackBtn.classList.add('active');
    themeWhiteBtn.classList.remove('active');
});

themeWhiteBtn.addEventListener('click', () => {
    document.body.classList.add('light-theme');
    themeWhiteBtn.classList.add('active');
    themeBlackBtn.classList.remove('active');
});

async function handleFileSelect(e, target) {
    const file = e.target.files[0];
    if (!file) return;

    statusBar.textContent = '読み込み中...';
    try {
        const text = await file.text();
        const json = JSON.parse(text);
        if (target === 'A') dataA = json;
        else dataB = json;

        statusBar.textContent = `ファイル ${target} 読み込み完了 (${json.length} items)`;
        searchInput.disabled = !dataA;

        if (currentMode === 'compare') compareData();
        else applyFilter();
    } catch (err) {
        console.error(err);
        statusBar.textContent = `エラー: ファイル ${target} の解析に失敗しました。`;
    }
}

fileInput1.addEventListener('change', (e) => handleFileSelect(e, 'A'));
fileInput2.addEventListener('change', (e) => handleFileSelect(e, 'B'));

function init() {
    updateTableHeader();
}
init();
