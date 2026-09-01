<?php
session_start();
require_once '../config.php';
require_once __DIR__ . '/admin_guard.php';
?>
<!DOCTYPE html>
<html lang="fa" dir="rtl">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>داشبورد مدیر</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Vazirmatn:wght@300;400;600;700&display=swap" rel="stylesheet">
    <style>
        :root{--bg:#0f172a;--card:#111827;--muted:#93a4b8;--accent:#3b82f6;--accent2:#22c55e;--border:#1f2937;--danger:#ef4444}
        *{box-sizing:border-box}
        body{margin:0;font-family:'Vazirmatn',system-ui,-apple-system,Segoe UI,Roboto,Ubuntu;direction:rtl;background:linear-gradient(135deg,#0f172a,#111827);color:#e5e7eb}
        header{display:flex;align-items:center;justify-content:space-between;padding:16px 24px;border-bottom:1px solid var(--border);background:#0b1220;position:sticky;top:0;z-index:10}
        .brand{display:flex;gap:12px;align-items:center}
        .brand .title{font-weight:700;font-size:18px}
        .nav{display:flex;flex-wrap:wrap;gap:8px}
        .nav button{background:#0c1a33;color:#cbd5e1;border:1px solid var(--border);padding:8px 12px;border-radius:8px;cursor:pointer}
        .nav button.active{background:var(--accent);color:#fff;border-color:transparent}
        main{padding:16px}
        .card{background:var(--card);border:1px solid var(--border);border-radius:12px;padding:16px;margin-bottom:16px}
        .table{width:100%;border-collapse:collapse}
        .table th,.table td{border-bottom:1px solid var(--border);padding:10px;text-align:right}
        .table th{color:#9fb3c8;font-weight:600;background:#0b1220}
        .actions{display:flex;gap:8px}
        .btn{padding:6px 10px;border-radius:8px;border:1px solid var(--border);background:#0c1a33;color:#cbd5e1;cursor:pointer}
        .btn.primary{background:var(--accent);color:#fff;border-color:transparent}
        .btn.success{background:var(--accent2);color:#052e16;border-color:transparent}
        .btn.danger{background:#1b0c0c;color:#fecaca;border-color:#3f1818}
        .search{display:flex;gap:8px;margin-bottom:12px}
        input,select{background:#0b1220;color:#e5e7eb;border:1px solid var(--border);border-radius:8px;padding:8px}
        input::placeholder{color:#748094}
        .hidden{display:none}
        footer{padding:16px;text-align:center;color:#748094}
    </style>
    <script>
        const API_URL = 'admin_api.php';
        const sections = [
            {key:'users', label:'کاربران'},
            {key:'businesses', label:'کسب‌وکارها'},
            {key:'services', label:'خدمات'},
            {key:'bookings', label:'رزروها'},
            {key:'marketers', label:'مارکترها'},
            {key:'schedules', label:'زمان‌بندی‌ها'},
            {key:'schedule_overrides', label:'تغییرات زمان‌بندی'},
            {key:'daily_schedules', label:'زمان‌بندی روزانه'},
            {key:'specific_time_slots', label:'اسلات‌های خاص'},
            {key:'payments', label:'پرداخت‌ها'},
            {key:'SubscriptionPlans', label:'پلن‌های اشتراک'}
        ];

        let activeSection = 'businesses';

        let pkMap = {};
        let selectedSet = new Set();
        async function fetchList(section){
            const resp = await fetch(`${API_URL}?action=list&table=${encodeURIComponent(section)}`);
            const data = await resp.json();
            if(!data.success) throw new Error(data.message || data.error || 'خطا در دریافت لیست');
            if(data.primaryKey) pkMap[section] = data.primaryKey;
            return data.data;
        }
        async function deleteRow(section,id){
            if(!confirm('حذف رکورد؟')) return;
            const resp = await fetch(API_URL,{
                method:'POST',
                headers:{'Content-Type':'application/json'},
                body:JSON.stringify({action:'delete',table:section,id})
            });
            const data = await resp.json();
            if(data.success){ loadSection(activeSection); }
            else alert(data.message || data.error || 'خطا در حذف');
        }
        function renderTable(section,rows){
            const container = document.getElementById('content');
            if(!rows || rows.length===0){
                container.innerHTML = `<div class="card">داده‌ای یافت نشد</div>`;
                return;
            }
            const headers = Object.keys(rows[0]);
            let html = `<div class="card"><div class="search"><input id="q" placeholder="جستجو..." oninput="filterRows()"/> <button class="btn success" onclick="openCreate()">ایجاد رکورد</button> <button id="bulkDelete" class="btn danger" onclick="deleteSelected()" disabled>حذف انتخاب‌شده</button></div>`;
            html += `<table class="table"><thead><tr><th><input type=\"checkbox\" id=\"selectAll\" onchange=\"toggleSelectAll(this.checked)\"/></th>` + headers.map(h=>`<th>${h}</th>`).join('') + `<th>عملیات</th></tr></thead><tbody id="tbody">`;
            html += rows.map(r=>{
                const tds = headers.map(h=>`<td>${r[h]===null?'—':String(r[h]).slice(0,200)}</td>`).join('');
                const pk = pkMap[section] || 'id';
                const rowId = r[pk];
                const checked = selectedSet.has(String(rowId)) ? 'checked' : '';
                return `<tr data-json='${JSON.stringify(r).replace(/'/g,"&#39;")}'>`+
                    `<td><input type=\"checkbox\" class=\"rowcheck\" data-id=\"${String(rowId)}\" onchange=\"toggleSelect('${section}',${JSON.stringify(rowId)},this.checked)\" ${checked}></td>`+
                    `${tds}<td class=\"actions\"><button class=\"btn\" onclick=\"openEdit('${section}',${JSON.stringify(rowId)})\">ویرایش</button><button class=\"btn danger\" onclick=\"deleteRow('${section}',${JSON.stringify(rowId)})\">حذف</button></td></tr>`;
            }).join('');
            html += `</tbody></table></div>`;
            container.innerHTML = html;
            updateSelectionUI();
        }
        function filterRows(){
            const q = document.getElementById('q').value.trim().toLowerCase();
            const rows = Array.from(document.querySelectorAll('#tbody tr'));
            rows.forEach(tr=>{
                const data = tr.getAttribute('data-json').toLowerCase();
                tr.style.display = data.includes(q)?'':'none';
            });
        }
        function openEdit(section,id){
            const tr = Array.from(document.querySelectorAll('#tbody tr')).find(r=>{
                const obj = JSON.parse(r.getAttribute('data-json'));
                const pk = pkMap[section] || 'id';
                return obj[pk]==id;
            });
            if(!tr) return alert('رکورد یافت نشد');
            const obj = JSON.parse(tr.getAttribute('data-json'));
            const form = document.getElementById('editForm');
            form.classList.remove('hidden');
            form.dataset.section = section;
            form.dataset.id = id;
            const jsonArea = document.getElementById('json');
            jsonArea.value = JSON.stringify(obj,null,2);
        }
        function openCreate(){
            const form = document.getElementById('editForm');
            form.classList.remove('hidden');
            form.dataset.section = activeSection;
            form.dataset.id = '';
            const jsonArea = document.getElementById('json');
            jsonArea.value = JSON.stringify({/* فیلدها را پر کنید */},null,2);
        }
        async function saveEdit(){
            const form = document.getElementById('editForm');
            const section = form.dataset.section;
            const idRaw = form.dataset.id;
            const id = idRaw ? idRaw : null; // حفظ نوع شناسه (عدد یا رشته)
            let payload;
            try{ payload = JSON.parse(document.getElementById('json').value); }
            catch(e){ return alert('JSON نامعتبر'); }
            // عدم به‌روزرسانی کلید اصلی هنگام ویرایش
            if(id){
                const pk = pkMap[section] || 'id';
                if (payload && Object.prototype.hasOwnProperty.call(payload, pk)) {
                    delete payload[pk];
                }
            }
            const body = id ? {action:'update',table:section,id,data:payload} : {action:'create',table:section,data:payload};
            const resp = await fetch(API_URL,{ method:'POST',headers:{'Content-Type':'application/json'}, body:JSON.stringify(body)});
            const data = await resp.json();
            if(data.success){ document.getElementById('editForm').classList.add('hidden'); loadSection(activeSection); }
            else alert(data.message || data.error || 'خطا در ذخیره');
        }
        function toggleSelect(section,id,checked){
            const key = String(id);
            if(checked) selectedSet.add(key); else selectedSet.delete(key);
            updateSelectionUI();
        }
        function toggleSelectAll(checked){
            const boxes = document.querySelectorAll('#tbody .rowcheck');
            boxes.forEach(cb=>{
                cb.checked = checked;
                const id = cb.dataset.id;
                if(checked) selectedSet.add(id); else selectedSet.delete(id);
            });
            updateSelectionUI();
        }
        function updateSelectionUI(){
            const btn = document.getElementById('bulkDelete');
            if(btn) btn.disabled = selectedSet.size===0;
        }
        async function deleteSelected(){
            if(selectedSet.size===0) return;
            if(!confirm(`حذف ${selectedSet.size} رکورد انتخاب‌شده؟`)) return;
            const ids = Array.from(selectedSet);
            try{
                const results = await Promise.all(ids.map(id=>
                    fetch(API_URL,{
                        method:'POST',headers:{'Content-Type':'application/json'},
                        body:JSON.stringify({action:'delete',table:activeSection,id})
                    }).then(r=>r.json())
                ));
                const ok = results.every(r=>r && r.success);
                if(!ok) alert('برخی رکوردها حذف نشدند یا خطا داشتند');
            }catch(e){
                alert('خطا در حذف دسته‌ای');
            }
            selectedSet.clear();
            loadSection(activeSection);
        }
        async function loadSection(section){
            activeSection = section;
            selectedSet = new Set();
            document.querySelectorAll('.nav button').forEach(b=>b.classList.toggle('active', b.dataset.key===section));
            const rows = await fetchList(section);
            renderTable(section,rows);
        }
        function logout(){
            fetch('admin_api.php?action=logout').then(()=>{ window.location.href='admin_login.php'; });
        }
        window.addEventListener('DOMContentLoaded',()=>{
            const nav = document.getElementById('nav');
            nav.innerHTML = sections.map(s=>`<button data-key="${s.key}" onclick="loadSection('${s.key}')" class="${s.key===activeSection?'active':''}">${s.label}</button>`).join('');
            loadSection(activeSection);
            loadHealth();
        });

        async function loadHealth(){
            try{
                const resp = await fetch('admin_api.php?action=health');
                const data = await resp.json();
                const el = document.getElementById('health');
                if(!data.success) { el.innerHTML = '<div class="card">بخش سلامت: خطا در دریافت اطلاعات</div>'; return; }
                const items = Object.entries(data.tables).map(([t,c])=>`<li>${t}: ${c===null?'— (جدول موجود نیست)':c}</li>`).join('');
                el.innerHTML = `<div class="card"><h3>وضعیت دیتابیس</h3><div>موتور: ${data.engine}</div><div>اتصال: ${data.connection}</div><ul>${items}</ul></div>`;
            }catch(e){
                const el = document.getElementById('health');
                el.innerHTML = '<div class="card">بخش سلامت: خطا در ارتباط با سرور</div>';
            }
        }
    </script>
</head>
<body>
    <header>
        <div class="brand">
            <div class="title">داشبورد مدیر</div>
        </div>
        <div>
            <button class="btn" onclick="logout()">خروج</button>
        </div>
    </header>
    <main>
        <div class="card">
            <div id="nav" class="nav"></div>
        </div>
        <div id="health"></div>
        <div id="content"></div>

        <div id="editForm" class="card hidden">
            <h3>ویرایش رکورد</h3>
            <textarea id="json" rows="10" style="width:100%"></textarea>
            <div style="margin-top:8px;display:flex;gap:8px">
                <button class="btn success" onclick="saveEdit()">ذخیره</button>
                <button class="btn" onclick="document.getElementById('editForm').classList.add('hidden')">انصراف</button>
            </div>
        </div>
    </main>
    <footer>
        فقط مدیر قادر به دسترسی است. لطفا رمز عبور را امن نگه دارید.
    </footer>
</body>
</html>