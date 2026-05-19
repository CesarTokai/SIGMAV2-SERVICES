# ¿Qué es el desenfoque de fondo (backdrop blur)?
Es un efecto visual donde el contenido detrás de un elemento (como un modal) se ve borroso, ayudando a enfocar la atención en el modal y dar una sensación moderna y elegante.

---

## 2. ¿Cómo se implementa con CSS?
Se usa la propiedad CSS `backdrop-filter: blur(6px);` sobre el overlay del modal. Ejemplo:

```css
.modal-overlay {
  position: fixed;
  top: 0; left: 0; width: 100vw; height: 100vh;
  background: rgba(0,0,0,0.5); /* oscurece el fondo */
  backdrop-filter: blur(6px);   /* desenfoca el fondo */
  display: flex; align-items: center; justify-content: center;
  z-index: 1000;
}
```

---

## 3. ¿Cómo interactúa con el DOM?
- El overlay (`.modal-overlay`) cubre toda la pantalla y se coloca encima del contenido.
- El modal real (`.modal-content`) está dentro del overlay.
- El desenfoque solo afecta lo que está “detrás” del overlay, es decir, el contenido de la página que no está dentro del modal.

---

## 4. Compatibilidad
- `backdrop-filter` funciona en la mayoría de navegadores modernos (Chrome, Edge, Safari, Firefox).
- No funciona en navegadores antiguos (IE, Edge Legacy).
- Para mayor compatibilidad, puedes usar un color de fondo semitransparente como fallback.

---

## 5. Mejores prácticas
- Usa un color de fondo semitransparente junto con el blur para oscurecer y desenfocar.
- No pongas un fondo blanco sólido en el body o en el contenedor principal, porque el desenfoque solo se nota si hay contenido visible detrás.
- Usa `backdrop-filter` solo en overlays que cubran el contenido que quieres desenfocar.

---

## 6. Ejemplo de estructura HTML
```html
<div class="contenido-principal">
  <!-- Todo el contenido de la página -->
</div>
<div class="modal-overlay">
  <div class="modal-content">
    <!-- El contenido del modal -->
  </div>
</div>
```

---

## 7. Problemas comunes
- Si el fondo se ve blanco, probablemente el body o el contenedor principal tiene un fondo blanco sólido.
- Si el desenfoque no se ve, revisa la compatibilidad del navegador y que el overlay esté sobre el contenido correcto.
- El desenfoque puede afectar el rendimiento en dispositivos lentos.

