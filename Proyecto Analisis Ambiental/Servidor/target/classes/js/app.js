async function fetchData(){
  const res = await fetch('/api/data');
  if(!res.ok) return [];
  return await res.json();
}

function formatTs(ts){
  const d = new Date(ts);
  return d.toLocaleString();
}

// Render tabla
async function renderTable(){
  const data = await fetchData();
  const tbody = document.querySelector('#readings tbody');
  if(!tbody) return;
  tbody.innerHTML = '';
  for(const r of data.slice().reverse()){
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td>${formatTs(r.ts)}</td>
      <td>${r.device||''}</td>
      <td>${r.sensors?.temp??''}</td>
      <td>${r.sensors?.hum??''}</td>
      <td>${r.sensors?.mqVolt??''}</td>
      <td>${r.sensors?.uvVolt??''}</td>
      <td>${r.sensors?.vbat??''}</td>`;
    tbody.appendChild(tr);
  }
}

// Render gráficas
async function renderCharts(){
  const data = await fetchData();
  if(!data.length) return;
  const labels = data.map(d=>formatTs(d.ts));
  const s = d => data.map(x=>x.sensors?x.sensors[d]:null);
  const cfg = (id,label,values,color) => new Chart(
    document.getElementById(id), {
      type:'line',
      data:{labels,datasets:[{label,data:values,borderColor:color,fill:false}]},
      options:{responsive:true,scales:{x:{ticks:{maxRotation:45,minRotation:45}}}}
    });
  cfg('chartTemp','Temp (°C)',s('temp'),'#e63946');
  cfg('chartHum','Humedad (%)',s('hum'),'#457b9d');
  cfg('chartMQ','MQ Volt',s('mqVolt'),'#f4a261');
  cfg('chartUV','UV Volt',s('uvVolt'),'#2a9d8f');
  cfg('chartVbat','Vbat (V)',s('vbat'),'#1d3557');
}

if(location.pathname.includes('metricas')) renderCharts();
else renderTable();
setInterval(()=>{ if(location.pathname.includes('metricas')) renderCharts(); else renderTable(); }, 5000);
