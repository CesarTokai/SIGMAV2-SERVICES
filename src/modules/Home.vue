<template>
  <div class="home-container">
    <!-- NAVBAR MEJORADA -->
    <nav class="navbar-landing">
      <b-container class="nav-container">
        <div class="nav-content">
          <div class="nav-logo">
            <img
                class="logo-img"
                src="../assets/sweet-delivery-logo-ongreen.svg"
                alt="SIGMAV2 Logo"
            />
          </div>
          <div class="nav-links">
            <button class="login-btn" @click="redirectToLogin">
              <i class="fi fi-rr-arrow-right"></i> Iniciar Sesión
            </button>
          </div>
        </div>
      </b-container>
    </nav>

    <!-- HERO SECTION -->
    <section class="hero-section">
      <b-container class="hero-container">
        <b-row class="hero-row" align-v="center">
          <b-col lg="6" class="hero-text">
            <h1 class="hero-title">
              Ayudando a <span class="highlight">microempresas</span>
              <br />desde hace meses
            </h1>
            <p class="hero-subtitle">
              Facilitaremos todo el proceso de gestión de tu empresa con nuestra plataforma intuitiva y confiable.
            </p>
            <div class="hero-buttons">
              <button class="btn-primary" @click="redirectToLogin">
                Iniciar Sesión
              </button>
            </div>
          </b-col>
          <b-col lg="6" class="hero-image">
            <img src="../assets/landing-assets/bro-in-car.svg" alt="Delivery" class="fade-in" />
          </b-col>
        </b-row>
      </b-container>
    </section>

    <!-- CLIENTS SECTION -->
    <section class="clients-section">
      <b-container>
        <div class="section-header">
          <h2>Nuestros Clientes</h2>
          <p>Más de 500 empresas confían en SIGMAV2</p>
        </div>
        <b-row class="clients-row">
          <b-col lg="2" md="3" sm="4" xs="6" class="client-col" v-for="logo in 7" :key="logo">
            <div class="client-logo">
              <img
                  :src="`../assets/landing-assets/fake-logos/logoipsum-${280 + (logo-1)*5}.svg`"
                  :alt="`Cliente ${logo}`"
              />
            </div>
          </b-col>
        </b-row>
      </b-container>
    </section>

    <!-- FEATURES SECTION -->
    <section class="features-section">
      <b-container>
        <div class="section-header">
          <h2>Administra tu empresa eficientemente</h2>
          <p>Descubre nuestras características principales</p>
        </div>
        <b-row class="features-row">
          <b-col lg="4" md="6" class="feature-col" v-for="feature in features" :key="feature.id">
            <div class="feature-card">
              <div class="feature-icon">
                <img :src="feature.icon" :alt="feature.title" />
              </div>
              <h5 class="feature-title">{{ feature.title }}</h5>
              <p class="feature-text">{{ feature.description }}</p>
            </div>
          </b-col>
        </b-row>
      </b-container>
    </section>

    <!-- TESTIMONIAL SECTION -->
    <section class="testimonial-section">
      <b-container>
        <b-row align-v="center">
          <b-col lg="5" class="testimonial-image">
            <div class="image-wrapper">
              <img
                  src="../assets/landing-assets/bussiness-man-resize.jpg"
                  alt="CEO"
                  class="testimonial-img"
              />
            </div>
          </b-col>
          <b-col lg="7" class="testimonial-text">
            <div class="testimonial-content">
              <div class="quote-icon">
                <i class="fi fi-rr-quote"></i>
              </div>
              <p class="testimonial-text-main">
                Gracias al uso de SIGMAV2, he logrado simplificar la gestión de mis clientes y mejorar significativamente nuestro servicio. Esta herramienta me ha permitido optimizar procesos, aumentar eficiencia e incrementar mis ventas.
              </p>
              <div class="testimonial-author">
                <h6 class="author-name">Cesar Uriel</h6>
                <p class="author-title">CEO de Givaudan</p>
              </div>
            </div>
          </b-col>
        </b-row>
      </b-container>
    </section>

    <!-- WHY US SECTION -->
    <section class="why-us-section">
      <b-container>
        <div class="section-header">
          <h2>¿Por qué elegir SIGMAV2?</h2>
          <p>Soluciones personalizadas que se adaptan a tus necesidades</p>
        </div>
        <b-row class="why-us-row">
          <b-col lg="4" md="6" class="why-us-col" v-for="reason in 3" :key="reason">
            <div class="why-us-card">
              <div class="why-us-image">
                <img
                    :src="reason === 1 ? '../assets/landing-assets/delivery-man.jpg' :
                           reason === 2 ? '../assets/landing-assets/truck-delivery.jpeg' :
                           '../assets/landing-assets/bussiness-talk.jpg'"
                    :alt="reason === 1 ? 'Repartidores' : reason === 2 ? 'Transportes' : 'Comunicación'"
                />
              </div>
              <div class="why-us-text">
                <p v-if="reason === 1">
                  Repartidores profesionales capacitados y comprometidos con la excelencia.
                </p>
                <p v-else-if="reason === 2">
                  Flota moderna de transportes para garantizar entrega segura.
                </p>
                <p v-else>
                  Comunicación constante para mejorar continuamente.
                </p>
              </div>
            </div>
          </b-col>
        </b-row>
      </b-container>
    </section>

    <!-- CTA SECTION -->
    <section class="cta-section">
      <b-container>
        <b-row align-v="center" class="cta-row">
          <b-col lg="6" class="cta-image">
            <img src="../assets/landing-assets/Devices-bro.svg" alt="Dispositivos" />
          </b-col>
          <b-col lg="6" class="cta-text">
            <h2>¿Listo para transformar tu negocio?</h2>
            <p>
              Únete a miles de empresas que ya están usando SIGMAV2 para optimizar su gestión.
            </p>
            <button class="btn-primary btn-large" @click="redirectToLogin">
              Iniciar Sesión
            </button>
          </b-col>
        </b-row>
      </b-container>
    </section>

    <!-- FOOTER -->
    <footer class="footer">
      <b-container>
        <p>&copy; 2025 SIGMAV2. Todos los derechos reservados.</p>
      </b-container>
    </footer>
  </div>
</template>

<script>
import { defineComponent } from "vue";

export default defineComponent({
  name: "Home",
  data() {
    return {
      features: [
        {
          id: 1,
          title: "Excelente Comunidad",
          description: "Nuestro equipo especializado en atención al cliente te apoya en todo momento.",
          icon: "../assets/landing-assets/community-svg.svg"
        },
        {
          id: 2,
          title: "Nacionalmente Conocidos",
          description: "Reconocidos por nuestro trabajo excepcional en distribución y logística.",
          icon: "../assets/landing-assets/buildings-svg.svg"
        },
        {
          id: 3,
          title: "Gestión Automatizada",
          description: "Software avanzado que automatiza procesos y maximiza tu productividad.",
          icon: "../assets/landing-assets/hands-svg.svg"
        }
      ]
    };
  },
   methods: {
     redirectToLogin() {
       this.$router.push({ name: "login" });
     },
   },
});
</script>

<style scoped>
/* Variables y estilos globales */
:root {
  --primary-color: #32705d;
  --primary-light: #4b937d;
  --primary-dark: #1e4236;
  --accent-color: #f39c12;
  --text-dark: #2c3e50;
  --text-light: #7f8c8d;
  --bg-light: #f8f9fa;
  --white: #ffffff;
  --shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
  --shadow-sm: 0 5px 15px rgba(0, 0, 0, 0.08);
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body {
  width: 100%;
  height: 100%;
}

.home-container {
  font-family: "Lexend", -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
  color: var(--text-dark);
  overflow-x: hidden;
}

/* NAVBAR */
.navbar-landing {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 80px;
  background: linear-gradient(135deg, var(--primary-color) 0%, var(--primary-light) 100%);
  box-shadow: var(--shadow-sm);
  z-index: 1000;
  display: flex;
  align-items: center;
}

.nav-container {
  width: 100%;
}

.nav-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
}

.nav-logo img {
  height: 50px;
  width: auto;
  transition: transform 0.3s ease;
}

.nav-logo img:hover {
  transform: scale(1.05);
}

.login-btn {
  background: var(--white);
  color: var(--primary-color);
  border: none;
  padding: 12px 24px;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.95rem;
}

.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-sm);
  background: #f0f0f0;
}

/* HERO SECTION */
.hero-section {
  margin-top: 80px;
  padding: 100px 0;
  background: linear-gradient(135deg, #f8f9fa 0%, #ffffff 100%);
  min-height: calc(100vh - 80px);
  display: flex;
  align-items: center;
}

.hero-container {
  width: 100%;
}

.hero-row {
  gap: 40px;
}

.hero-title {
  font-size: 3.5rem;
  font-weight: 800;
  line-height: 1.2;
  margin-bottom: 20px;
  color: var(--text-dark);
}

.highlight {
  color: var(--primary-color);
  background: linear-gradient(120deg, var(--primary-color) 0%, var(--primary-light) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-subtitle {
  font-size: 1.2rem;
  color: var(--text-light);
  margin-bottom: 40px;
  line-height: 1.6;
}

.hero-buttons {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
}

.btn-primary {
  background: linear-gradient(135deg, var(--primary-color) 0%, var(--primary-light) 100%);
  color: var(--white);
  border: none;
  padding: 14px 40px;
  border-radius: 50px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: var(--shadow-sm);
}

.btn-primary:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow);
}

.btn-secondary {
  background: var(--white);
  color: var(--primary-color);
  border: 2px solid var(--primary-color);
  padding: 12px 40px;
  border-radius: 50px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-secondary:hover {
  background: var(--primary-color);
  color: var(--white);
  transform: translateY(-3px);
}

.hero-image {
  display: flex;
  justify-content: center;
  align-items: center;
}

.hero-image img {
  max-width: 100%;
  height: auto;
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0px); }
  50% { transform: translateY(20px); }
}

.fade-in {
  animation: fadeInUp 0.8s ease-out;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* CLIENTS SECTION */
.clients-section {
  padding: 80px 0;
  background: var(--white);
}

.section-header {
  text-align: center;
  margin-bottom: 60px;
}

.section-header h2 {
  font-size: 2.5rem;
  font-weight: 800;
  margin-bottom: 15px;
  color: var(--text-dark);
}

.section-header p {
  font-size: 1.1rem;
  color: var(--text-light);
}

.clients-row {
  display: flex;
  align-items: center;
  justify-content: center;
}

.client-col {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-bottom: 30px;
}

.client-logo {
  width: 100%;
  height: 80px;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 10px;
  transition: all 0.3s ease;
  opacity: 0.7;
}

.client-logo:hover {
  opacity: 1;
  transform: scale(1.1);
}

.client-logo img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

/* FEATURES SECTION */
.features-section {
  padding: 80px 0;
  background: #f8f9fa;
}

.features-row {
  display: flex;
  gap: 30px;
}

.feature-col {
  margin-bottom: 30px;
}

.feature-card {
  background: var(--white);
  padding: 40px 30px;
  border-radius: 12px;
  text-align: center;
  height: 100%;
  transition: all 0.3s ease;
  box-shadow: var(--shadow-sm);
  border: 2px solid transparent;
}

.feature-card:hover {
  transform: translateY(-10px);
  box-shadow: var(--shadow);
  border-color: var(--primary-color);
}

.feature-icon {
  width: 80px;
  height: 80px;
  margin: 0 auto 20px;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, var(--primary-color), var(--primary-light));
  border-radius: 50%;
}

.feature-icon img {
  width: 50%;
  height: 50%;
  filter: brightness(0) invert(1);
}

.feature-title {
  font-size: 1.3rem;
  font-weight: 700;
  margin-bottom: 15px;
  color: var(--text-dark);
}

.feature-text {
  color: var(--text-light);
  line-height: 1.6;
  font-size: 0.95rem;
}

/* TESTIMONIAL SECTION */
.testimonial-section {
  padding: 100px 0;
  background: var(--white);
}

.testimonial-image {
  display: flex;
  justify-content: center;
  margin-bottom: 30px;
}

.image-wrapper {
  position: relative;
  width: 280px;
  height: 280px;
  border-radius: 15px;
  overflow: hidden;
  box-shadow: var(--shadow);
}

.testimonial-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.testimonial-text {
  display: flex;
  align-items: center;
}

.testimonial-content {
  padding: 30px;
}

.quote-icon {
  font-size: 3rem;
  color: var(--primary-color);
  margin-bottom: 20px;
}

.testimonial-text-main {
  font-size: 1.2rem;
  line-height: 1.8;
  color: var(--text-dark);
  margin-bottom: 30px;
  font-style: italic;
}

.testimonial-author {
  margin-top: 30px;
}

.author-name {
  font-size: 1.1rem;
  font-weight: 700;
  color: var(--primary-color);
  margin-bottom: 5px;
}

.author-title {
  font-size: 0.95rem;
  color: var(--text-light);
}

/* WHY US SECTION */
.why-us-section {
  padding: 80px 0;
  background: #f8f9fa;
}

.why-us-row {
  display: flex;
  gap: 30px;
}

.why-us-col {
  margin-bottom: 30px;
}

.why-us-card {
  background: var(--white);
  border-radius: 12px;
  overflow: hidden;
  box-shadow: var(--shadow-sm);
  transition: all 0.3s ease;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.why-us-card:hover {
  transform: translateY(-10px);
  box-shadow: var(--shadow);
}

.why-us-image {
  width: 100%;
  height: 200px;
  overflow: hidden;
  position: relative;
}

.why-us-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.why-us-card:hover .why-us-image img {
  transform: scale(1.1);
}

.why-us-text {
  padding: 30px 25px;
  flex: 1;
  display: flex;
  align-items: center;
}

.why-us-text p {
  font-size: 1rem;
  line-height: 1.6;
  color: var(--text-dark);
}

/* CTA SECTION */
.cta-section {
  padding: 100px 0;
  background: linear-gradient(135deg, var(--primary-color) 0%, var(--primary-light) 100%);
  color: var(--white);
}

.cta-row {
  gap: 60px;
}

.cta-image {
  display: flex;
  justify-content: center;
}

.cta-image img {
  max-width: 100%;
  height: auto;
}

.cta-text h2 {
  font-size: 2.5rem;
  font-weight: 800;
  margin-bottom: 20px;
}

.cta-text p {
  font-size: 1.1rem;
  margin-bottom: 40px;
  line-height: 1.6;
  opacity: 0.95;
}

.btn-large {
  padding: 16px 50px !important;
  font-size: 1.1rem !important;
}

/* FOOTER */
.footer {
  background: var(--primary-dark);
  color: var(--white);
  padding: 30px 0;
  text-align: center;
}

.footer p {
  margin: 0;
  font-size: 0.95rem;
  opacity: 0.9;
}

/* RESPONSIVE */
@media (max-width: 992px) {
  .hero-title {
    font-size: 2.5rem;
  }

  .hero-subtitle {
    font-size: 1rem;
  }

  .section-header h2 {
    font-size: 2rem;
  }

  .cta-text h2 {
    font-size: 2rem;
  }

  .feature-card,
  .why-us-card {
    margin-bottom: 20px;
  }
}

@media (max-width: 768px) {
  .navbar-landing {
    height: 70px;
  }

  .hero-section {
    padding: 60px 0;
    margin-top: 70px;
  }

  .hero-title {
    font-size: 2rem;
  }

  .hero-buttons {
    flex-direction: column;
  }

  .btn-primary, .btn-secondary {
    width: 100%;
    text-align: center;
  }

  .section-header h2 {
    font-size: 1.8rem;
  }

  .clients-row {
    flex-wrap: wrap;
  }

  .client-col {
    flex: 0 0 50%;
  }

  .testimonial-section {
    padding: 60px 0;
  }

  .image-wrapper {
    width: 220px;
    height: 220px;
  }

  .cta-section {
    padding: 60px 0;
  }

  .cta-row {
    gap: 30px;
    flex-direction: column;
  }
}

@media (max-width: 576px) {
  .nav-logo img {
    height: 40px;
  }

  .hero-title {
    font-size: 1.8rem;
  }

  .hero-subtitle {
    font-size: 0.95rem;
  }

  .section-header h2 {
    font-size: 1.5rem;
  }

  .feature-card {
    padding: 25px 20px;
  }

  .client-col {
    flex: 0 0 100%;
  }

  .client-logo {
    height: 60px;
  }

  .image-wrapper {
    width: 180px;
    height: 180px;
  }

  .testimonial-content {
    padding: 20px;
  }

  .testimonial-text-main {
    font-size: 1rem;
  }

  .cta-text h2 {
    font-size: 1.5rem;
  }

  .cta-text p {
    font-size: 0.95rem;
  }
}
</style>