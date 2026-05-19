<template>
  <div id="app">
    <router-view />
  </div>
</template>

<style>
* {
  padding: 0;
  margin: 0;
  box-sizing: border-box;
}

html,
body {
  width: 100%;
  height: 100%;
}

body {
  font-family: "Lexend", sans-serif !important;
}
</style>

<script>
import { defineComponent } from "vue";

import { createContrast } from "./utils/createContrast";
import { lightenColor } from "./utils/lightenColor";

export default defineComponent({
  name: "App",
  methods: {
    configureColors() {
      // busca el color en el local storage
      let highOpacity = 0.5;
      let lowOpacity = 0.9;
      let colorHex = localStorage.getItem("color");
      let logo = localStorage.getItem("logo");

      // color no existe
      if (!colorHex || colorHex === "null") {
        colorHex = "#32705D";
      }

      document.documentElement.style.setProperty("--primary-color", colorHex);
      document.documentElement.style.setProperty(
          "--primary-color-high-opacity",
          lightenColor(colorHex, highOpacity)
      );
      document.documentElement.style.setProperty(
          "--primary-color-low-opacity",
          lightenColor(colorHex, lowOpacity)
      );
      document.documentElement.style.setProperty(
          "--primary-color-text",
          createContrast(colorHex)
      );

      // cambiar logo
      // cambiar logo
      if (logo) {
        // Elimina todos los favicons existentes
        const links = document.querySelectorAll("link[rel*='icon']");
        links.forEach((link) => link.parentNode.removeChild(link));

        // Crea un nuevo favicon
        const link = document.createElement("link");
        link.rel = "icon";
        link.type = "image/png";
        link.href = `data:image/png;base64,${logo}`;

        // Agrega el favicon al head
        document.head.appendChild(link);
      }
    },
  },
  created() {
    this.configureColors();
  },
});
</script>