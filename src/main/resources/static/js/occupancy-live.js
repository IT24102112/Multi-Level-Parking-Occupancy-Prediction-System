(function() {
  // Occupancy-only live updater: uses STOMP/SockJS if available, falls back to polling

  function normalize(s){ return (s||'').trim().toLowerCase(); }

  function findCardByLevel(levelName){
    const cards = document.querySelectorAll('.level-card');
    const target = normalize(levelName);
    for(const c of cards){
      const h = c.querySelector('h3');
      if(!h) continue;
      if(normalize(h.textContent) === target) return c;
    }
    return null;
  }

  function flashCard(card){
    try{
      const prevTransition = card.style.transition;
      const prevTransform = card.style.transform;
      const prevBox = card.style.boxShadow;
      card.style.transition = 'transform 150ms ease, box-shadow 300ms ease';
      card.style.transform = 'translateY(-6px)';
      card.style.boxShadow = '0 12px 24px rgba(0,0,0,0.08), 0 0 0 6px rgba(76,175,80,0.12)';
      setTimeout(()=>{
        card.style.transform = prevTransform || '';
        card.style.boxShadow = prevBox || '';
        // restore transition shortly after
        setTimeout(()=>{ card.style.transition = prevTransition || ''; }, 300);
      }, 350);
    }catch(e){/* ignore */}
  }

  function updateCardWithPayload(payload){
    const levelName = payload.levelName || payload.level_name || payload.level || payload.levelname;
    if(!levelName) return;
    const card = findCardByLevel(levelName);
    if(!card) return;

    const current = Number(payload.currentOccupancy ?? payload.current_occupancy ?? payload.current ?? 0);
    const total = Number(payload.totalSlots ?? payload.total_slots ?? payload.total ?? 0);
    const available = Number(payload.availableSlots ?? payload.available ?? (total - current));

    // availability main span
    const availSpan = card.querySelector('.availability-main span');
    if(availSpan) availSpan.textContent = String(available);

    // toggle full state and badge accessibility
    const badge = card.querySelector('.full-badge');
    if (available <= 0) {
      card.classList.add('is-full');
      if (badge) {
        badge.setAttribute('aria-hidden', 'false');
        badge.setAttribute('role', 'status');
      }
    } else {
      card.classList.remove('is-full');
      if (badge) {
        badge.setAttribute('aria-hidden', 'true');
        badge.removeAttribute('role');
      }
    }

    // level-sub-info strongs: first = current, second = total
    const strongs = card.querySelectorAll('.level-sub-info strong');
    if(strongs && strongs.length >= 1) strongs[0].textContent = String(current);
    if(strongs && strongs.length >= 2) strongs[1].textContent = String(total);

    // progress bar
    const bar = card.querySelector('.occ-bar-fill');
    if(bar && total > 0){
      const pct = Math.max(0, Math.min(100, (current*100)/total));
      bar.style.width = pct + '%';
    }

    // optional background color
    if(payload.occupancyColor){
      card.style.background = `linear-gradient(135deg, ${payload.occupancyColor} 0%, ${payload.occupancyColor}cc 100%)`;
    }

    // visual cue
    flashCard(card);
  }

  async function pollOnce(){
    try{
      const res = await fetch('/api/occupancy', {cache:'no-store'});
      if(!res.ok) return;
      const levels = await res.json();
      if(Array.isArray(levels)){
        levels.forEach(l => {
          updateCardWithPayload({
            levelName: l.levelName ?? l.level_name ?? l.level,
            currentOccupancy: l.currentOccupancy ?? l.current_occupancy ?? l.current,
            totalSlots: l.totalSlots ?? l.total_slots ?? l.total,
            availableSlots: (l.totalSlots ?? l.total_slots ?? l.total) - (l.currentOccupancy ?? l.current_occupancy ?? l.current),
            occupancyColor: l.occupancyColor
          });
        });
      }
    }catch(e){
      console.debug('occupancy poll failed', e);
    }
  }

  // WebSocket with reconnect/backoff
  let reconnectAttempts = 0;
  const maxReconnectDelay = 30000;
  let stompClient = null;
  let sock = null;

  function scheduleReconnect(){
    reconnectAttempts++;
    const delay = Math.min(maxReconnectDelay, Math.round(1000 * Math.pow(1.5, reconnectAttempts)));
    setTimeout(()=>{ connectWebSocket(); }, delay);
  }

  function connectWebSocket(){
    if(typeof SockJS === 'undefined' || typeof Stomp === 'undefined') return false;
    try{
      sock = new SockJS('/ws/occupancy');
      stompClient = Stomp.over(sock);
      if(stompClient) stompClient.debug = null;

      stompClient.connect({}, function(){
        reconnectAttempts = 0;
        // poll once on connect to ensure full sync
        pollOnce();
        stompClient.subscribe('/topic/occupancy', function(msg){
          try{
            const payload = JSON.parse(msg.body);
            updateCardWithPayload(payload);
            // quick follow-up poll shortly after to ensure full server state
            setTimeout(()=>{ try{ pollOnce(); }catch(e){} }, 200);
          }catch(e){ /* ignore */ }
        });
      }, function(){
        scheduleReconnect();
      });

      sock.onclose = function(){ scheduleReconnect(); };
      return true;
    }catch(e){
      scheduleReconnect();
      return false;
    }
  }

  document.addEventListener('DOMContentLoaded', function(){
    // initial poll to populate quickly
    pollOnce();
    // faster polling every 2s as a safety net
    setInterval(pollOnce, 2000);
    // try websocket first for immediate pushes
    connectWebSocket();
  });
})();
