# Setup Completo: Aplicación Flutter para Escaneo de Marbetes en SIGMAV2

**Versión:** 1.0.0  
**Fecha:** 2026-03-24  
**Módulo:** Mobile Client - Escaneo de Marbetes con QR/Código de Barras  
**Stack:** Flutter, Dart, REST API (SIGMAV2-SERVICES Java 21)

---

## 📋 Índice

1. [QR vs Código de Barras](#qr-vs-código-de-barras)
2. [Estructura de Carpetas](#estructura-de-carpetas-recomendada)
3. [Setup Inicial del Proyecto Flutter](#setup-inicial-del-proyecto-flutter)
4. [Dependencias Requeridas](#dependencias-requeridas)
5. [Configuración de Entorno](#configuración-de-entorno)
6. [Implementación del Escaneo](#implementación-del-escaneo)
7. [Servicio de API](#servicio-de-api)
8. [Pantallas Principales](#pantallas-principales)
9. [Modelos de Datos](#modelos-de-datos)
10. [Ejemplos de Uso](#ejemplos-de-uso)
11. [Testing](#testing-manual)

---

## QR vs Código de Barras

### 🎯 Recomendación: **QR (Mejor para este caso)**

| Aspecto | QR | Código de Barras |
|--------|-----|------------------|
| **Capacidad de datos** | Hasta 4,296 caracteres alfanuméricos | Max 48 caracteres numéricos |
| **Velocidad de escaneo** | Rápido (cualquier ángulo) | Requiere alineación perfecta |
| **Información embebida** | Número marbete + metadata | Solo número |
| **Tolerancia a daño** | 30% del código puede estar dañado | Menos tolerante |
| **Impresión en etiquetas pequeñas** | ✅ Perfecto | ❌ Poco legible en espacios chicos |
| **Redundancia** | Alta (detecta errores) | Baja |

### **Decisión Final: QR**
- **Por qué:** Mejor tolerancia a daño (importante en almacenes), velocidad, y cabe en etiquetas pequeñas.
- **Estrategia:** Codificar solo el **número consecutivo del marbete** (ej: "42"), nada más.
- **API:** GET `/api/sigmav2/labels/{numeroMarbete}` retorna toda la info.

---

## Estructura de Carpetas Recomendada

Organiza tu proyecto Flutter con separación clara de responsabilidades:

```
sigmav2_mobile_app/
├── lib/
│   ├── main.dart                              # Entry point
│   ├── config/
│   │   ├── app_config.dart                    # URLs, timeouts, config
│   │   ├── route_config.dart                  # Rutas (GoRouter)
│   │   └── theme.dart                         # Estilos globales
│   ├── models/
│   │   ├── marbete_model.dart                 # Entidad Marbete
│   │   ├── user_model.dart                    # Entidad User (login)
│   │   ├── api_response_model.dart            # Wrapper genérico de API
│   │   └── error_model.dart                   # Manejo de errores
│   ├── services/
│   │   ├── api_service.dart                   # Cliente HTTP + interceptores
│   │   ├── auth_service.dart                  # Gestión JWT
│   │   ├── marbete_service.dart               # Lógica de negocio - Marbetes
│   │   ├── qr_scanner_service.dart            # Integración con scanner
│   │   └── storage_service.dart               # Almacenamiento local (SharedPrefs)
│   ├── providers/
│   │   ├── auth_provider.dart                 # State (Riverpod/Provider)
│   │   ├── marbete_provider.dart              # State de marbetes
│   │   └── ui_provider.dart                   # Estado UI (loading, errores)
│   ├── screens/
│   │   ├── auth/
│   │   │   ├── login_screen.dart              # Login con credenciales
│   │   │   └── login_form.dart                # Formulario reutilizable
│   │   ├── home/
│   │   │   └── home_screen.dart               # Menú principal
│   │   ├── marbete/
│   │   │   ├── scanner_screen.dart            # Escaneo QR en vivo
│   │   │   ├── marbete_detail_screen.dart     # Detalles del marbete escaneado
│   │   │   ├── count_c1_screen.dart           # Registrar conteo C1
│   │   │   ├── count_c2_screen.dart           # Registrar conteo C2
│   │   │   └── marbete_list_screen.dart       # Histórico (opcional)
│   │   └── error/
│   │       └── error_screen.dart              # Pantalla de errores
│   ├── widgets/
│   │   ├── custom_app_bar.dart                # AppBar reutilizable
│   │   ├── custom_button.dart                 # Botones estilizados
│   │   ├── loading_overlay.dart               # Overlay de carga
│   │   ├── error_snackbar.dart                # Snackbar de errores
│   │   └── qr_display_widget.dart             # Mostrar QR escaneado
│   ├── utils/
│   │   ├── constants.dart                     # Constantes (URLs, timeouts)
│   │   ├── logger.dart                        # Logging
│   │   ├── validators.dart                    # Validadores
│   │   └── extensions.dart                    # Extensiones (String, int, etc)
│   └── core/
│       ├── exceptions.dart                    # Excepciones custom
│       └── result.dart                        # Result<T> para manejo de errores
├── assets/
│   ├── images/
│   │   └── logo.png
│   ├── icons/
│   │   └── qr_icon.svg
│   └── strings/
│       └── strings_es.json                    # Internacionalización (i18n)
├── test/
│   ├── unit/
│   │   ├── services/
│   │   │   └── marbete_service_test.dart
│   │   └── models/
│   │       └── marbete_model_test.dart
│   └── integration/
│       └── scanner_integration_test.dart
├── pubspec.yaml                               # Dependencias
├── pubspec.lock
├── .env                                       # Variables de entorno (NOT in git)
├── .env.example                               # Plantilla de .env
├── .gitignore
└── README.md
```

---

## Setup Inicial del Proyecto Flutter

### 1. Crear el Proyecto

```bash
# En PowerShell (Windows)
flutter create sigmav2_mobile_app

cd sigmav2_mobile_app

# Verificar que todo está bien
flutter doctor
```

### 2. Configurar `.env` para URLs del Backend

**Archivo: `.env`** (NO commitearlo)
```
API_BASE_URL=http://192.168.1.100:8080/api/sigmav2
API_TIMEOUT_SECONDS=30
LOG_LEVEL=DEBUG
```

**Archivo: `.env.example`** (para documentar)
```
API_BASE_URL=http://tu-ip-servidor:8080/api/sigmav2
API_TIMEOUT_SECONDS=30
LOG_LEVEL=DEBUG
```

---

## Dependencias Requeridas

Actualiza `pubspec.yaml`:

```yaml
name: sigmav2_mobile_app
description: Cliente mobile para escaneo de marbetes en SIGMAV2
publish_to: 'none'

version: 1.0.0+1

environment:
  sdk: '>=3.0.0 <4.0.0'

dependencies:
  flutter:
    sdk: flutter

  # HTTP y Networking
  http: ^1.1.0
  dio: ^5.3.1                          # (Alternativa: mejor con interceptores)

  # State Management
  riverpod: ^2.4.0                     # (O Provider: ^6.0.0)
  flutter_riverpod: ^2.4.0

  # Escaneo QR/Código de Barras
  mobile_scanner: ^3.5.0              # (Recomendado: mejor cámara + detección)
  # Alternativa: qr_code_scanner: ^1.0.1

  # Almacenamiento Local
  shared_preferences: ^2.2.0
  hive: ^2.2.3
  hive_flutter: ^1.1.0

  # Rutas y Navegación
  go_router: ^11.0.0                  # (Moderno y sencillo)

  # Utilidades
  flutter_dotenv: ^5.1.0              # Variables de entorno
  logger: ^2.0.1                       # Logging
  get_it: ^7.5.0                       # Service Locator / DI
  freezed_annotation: ^2.4.1           # Modelos inmutables

  # JSON Serialization
  json_annotation: ^4.8.0
  json_serializable: ^6.7.0

  # Internacionalización (i18n)
  easy_localization: ^3.0.1

  # Validación de formularios
  form_validator: ^0.0.2

dev_dependencies:
  flutter_test:
    sdk: flutter

  flutter_lints: ^3.0.0
  freezed: ^2.4.1
  build_runner: ^2.4.4
  json_serializable: ^6.7.0

flutter:
  uses-material-design: true

  assets:
    - assets/images/
    - assets/icons/
    - assets/strings/

  # Opcional: Agregar fuentes personalizadas
  fonts:
    - family: RobotoMono
      fonts:
        - asset: assets/fonts/RobotoMono-Regular.ttf
        - asset: assets/fonts/RobotoMono-Bold.ttf
          weight: 700
```

### Instalar Dependencias

```bash
flutter pub get

# Generar modelos con Freezed (si usas Freezed)
dart run build_runner build

# Limpiar cache si hay problemas
flutter clean
flutter pub get
```

---

## Configuración de Entorno

### 1. **`lib/config/app_config.dart`**

```dart
import 'package:flutter_dotenv/flutter_dotenv.dart';

class AppConfig {
  static late String apiBaseUrl;
  static late int apiTimeoutSeconds;
  static late String logLevel;

  static Future<void> init() async {
    await dotenv.load(fileName: ".env");

    apiBaseUrl = dotenv.env['API_BASE_URL'] ?? 'http://localhost:8080/api/sigmav2';
    apiTimeoutSeconds = int.parse(dotenv.env['API_TIMEOUT_SECONDS'] ?? '30');
    logLevel = dotenv.env['LOG_LEVEL'] ?? 'INFO';
  }

  /// URLs de los endpoints del backend
  static const String loginEndpoint = '/auth/login';
  static const String getMarbeteEndpoint = '/labels'; // GET /labels/{numeroMarbete}
  static const String countC1Endpoint = '/labels/counts/c1';
  static const String countC2Endpoint = '/labels/counts/c2';
}
```

### 2. **`lib/utils/constants.dart`**

```dart
class AppConstants {
  // Tiempos
  static const int apiTimeoutSeconds = 30;
  static const int debounceMilliseconds = 300;

  // Patrones
  static const String qrPattern = r'^\d+$'; // Solo números (número del marbete)

  // Storage Keys
  static const String tokenKey = 'jwt_token';
  static const String userKey = 'user_data';
  static const String lastScannedKey = 'last_scanned_marbete';

  // UI
  static const int maxRetriesOnError = 3;
  static const Duration toastDuration = Duration(seconds: 3);
}
```

---

## Implementación del Escaneo

### 1. **`lib/services/qr_scanner_service.dart`** - Integración de Escaneo

```dart
import 'package:mobile_scanner/mobile_scanner.dart';

class QRScannerService {
  late MobileScannerController scannerController;

  Future<void> initialize() async {
    scannerController = MobileScannerController(
      autoStart: false,
      torchEnabled: false,
      formats: [BarcodeFormat.qrCode, BarcodeFormat.code128], // Soportar ambos
    );
    await scannerController.start();
  }

  void toggleTorch() {
    scannerController.toggleTorch();
  }

  void restartScanning() {
    scannerController.reset();
  }

  Future<void> dispose() async {
    await scannerController.dispose();
  }

  /// Stream que emite valores escaneados
  Stream<String> get scannedBarcodes => scannerController.barcodes
      .map((barcode) => barcode.rawValue ?? '')
      .where((value) => value.isNotEmpty);
}
```

### 2. **`lib/screens/marbete/scanner_screen.dart`** - Pantalla de Escaneo

```dart
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:mobile_scanner/mobile_scanner.dart';
import '../../services/qr_scanner_service.dart';
import '../../providers/marbete_provider.dart';

class ScannerScreen extends ConsumerStatefulWidget {
  const ScannerScreen({Key? key}) : super(key: key);

  @override
  ConsumerState<ScannerScreen> createState() => _ScannerScreenState();
}

class _ScannerScreenState extends ConsumerState<ScannerScreen> {
  late QRScannerService scannerService;
  bool isTorchOn = false;
  bool isProcessing = false;

  @override
  void initState() {
    super.initState();
    scannerService = QRScannerService();
    scannerService.initialize();
    _setupScannedBarcodeListener();
  }

  void _setupScannedBarcodeListener() {
    scannerService.scannedBarcodes.listen(
      (scannedValue) async {
        if (!isProcessing) {
          setState(() => isProcessing = true);

          try {
            // Validar que sea solo números
            if (RegExp(r'^\d+$').hasMatch(scannedValue)) {
              int numeroMarbete = int.parse(scannedValue);

              // Consultar marbete desde API
              await ref.read(marbeteProvider.notifier).fetchMarbete(numeroMarbete);

              // Navegar a detalles
              if (mounted) {
                Navigator.of(context).pushNamed(
                  '/marbete-detail',
                  arguments: numeroMarbete,
                );
              }
            } else {
              _showError('QR inválido. Debe contener solo números.');
            }
          } catch (e) {
            _showError('Error al procesar QR: $e');
          } finally {
            setState(() => isProcessing = false);
          }
        }
      },
    );
  }

  void _showError(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: Colors.red,
        duration: const Duration(seconds: 3),
      ),
    );
  }

  @override
  void dispose() {
    scannerService.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Escanear Marbete'),
        actions: [
          IconButton(
            icon: Icon(isTorchOn ? Icons.flashlight_on : Icons.flashlight_off),
            onPressed: () {
              scannerService.toggleTorch();
              setState(() => isTorchOn = !isTorchOn);
            },
          ),
        ],
      ),
      body: Stack(
        children: [
          // Cámara
          MobileScanner(
            controller: scannerService.scannerController,
            onDetect: (capture) {
              final List<Barcode> barcodes = capture.barcodes;
              for (final barcode in barcodes) {
                debugPrint('Barcode found! ${barcode.rawValue}');
              }
            },
          ),

          // Overlay con rectángulo
          Positioned.fill(
            child: Stack(
              alignment: Alignment.center,
              children: [
                // Oscurecer bordes
                Container(
                  color: Colors.black.withOpacity(0.5),
                ),
                // Ventana de escaneo
                Container(
                  width: 250,
                  height: 250,
                  decoration: BoxDecoration(
                    border: Border.all(color: Colors.green, width: 3),
                    borderRadius: BorderRadius.circular(10),
                  ),
                ),
              ],
            ),
          ),

          // Indicador de procesamiento
          if (isProcessing)
            const Center(
              child: CircularProgressIndicator(
                valueColor: AlwaysStoppedAnimation<Color>(Colors.green),
              ),
            ),

          // Instrucciones
          Positioned(
            bottom: 50,
            left: 0,
            right: 0,
            child: Center(
              child: Text(
                'Alinea el QR dentro del cuadro',
                style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                  color: Colors.white,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
```

---

## Servicio de API

### **`lib/services/api_service.dart`** - Cliente HTTP con Interceptores

```dart
import 'package:dio/dio.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import '../config/app_config.dart';
import '../utils/logger.dart';

class ApiService {
  late Dio _dio;

  ApiService() {
    _initializeDio();
  }

  void _initializeDio() {
    _dio = Dio(
      BaseOptions(
        baseUrl: AppConfig.apiBaseUrl,
        connectTimeout: Duration(seconds: AppConfig.apiTimeoutSeconds),
        receiveTimeout: Duration(seconds: AppConfig.apiTimeoutSeconds),
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json',
        },
      ),
    );

    // Interceptor para agregar JWT
    _dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) {
          final token = _getStoredToken();
          if (token != null) {
            options.headers['Authorization'] = 'Bearer $token';
          }
          AppLogger.info('📤 REQUEST: ${options.method} ${options.path}');
          return handler.next(options);
        },
        onResponse: (response, handler) {
          AppLogger.info('📥 RESPONSE: ${response.statusCode} - ${response.requestOptions.path}');
          return handler.next(response);
        },
        onError: (error, handler) {
          AppLogger.error('❌ ERROR: ${error.message}');
          return handler.next(error);
        },
      ),
    );
  }

  // Obtener marbete por número
  Future<Map<String, dynamic>> getMarbete(int numeroMarbete) async {
    try {
      final response = await _dio.get('/labels/$numeroMarbete');
      return response.data;
    } on DioException catch (e) {
      _handleDioError(e);
      rethrow;
    }
  }

  // Registrar conteo C1
  Future<Map<String, dynamic>> registerCountC1(int labelId, int countValue) async {
    try {
      final response = await _dio.post(
        '/labels/counts/c1',
        data: {
          'labelId': labelId,
          'countValue': countValue,
        },
      );
      return response.data;
    } on DioException catch (e) {
      _handleDioError(e);
      rethrow;
    }
  }

  // Registrar conteo C2
  Future<Map<String, dynamic>> registerCountC2(int labelId, int countValue) async {
    try {
      final response = await _dio.post(
        '/labels/counts/c2',
        data: {
          'labelId': labelId,
          'countValue': countValue,
        },
      );
      return response.data;
    } on DioException catch (e) {
      _handleDioError(e);
      rethrow;
    }
  }

  // Login
  Future<String> login(String email, String password) async {
    try {
      final response = await _dio.post(
        '/auth/login',
        data: {
          'email': email,
          'password': password,
        },
      );
      return response.data['token'];
    } on DioException catch (e) {
      _handleDioError(e);
      rethrow;
    }
  }

  void _handleDioError(DioException error) {
    if (error.response != null) {
      AppLogger.error('Error ${error.response?.statusCode}: ${error.response?.data}');
    } else {
      AppLogger.error('Network Error: ${error.message}');
    }
  }

  String? _getStoredToken() {
    // Implementar con SharedPreferences
    // return await _storageService.getToken();
    return null; // TODO: Implementar
  }
}
```

### **`lib/services/marbete_service.dart`** - Lógica de Negocio

```dart
import 'api_service.dart';
import '../models/marbete_model.dart';

class MarbeteService {
  final ApiService apiService;

  MarbeteService(this.apiService);

  Future<MarbeteModel> getMarbete(int numeroMarbete) async {
    try {
      final data = await apiService.getMarbete(numeroMarbete);
      return MarbeteModel.fromJson(data);
    } catch (e) {
      throw Exception('Error al obtener marbete: $e');
    }
  }

  Future<Map<String, dynamic>> submitCountC1(int labelId, int countValue) async {
    try {
      return await apiService.registerCountC1(labelId, countValue);
    } catch (e) {
      throw Exception('Error al registrar conteo C1: $e');
    }
  }

  Future<Map<String, dynamic>> submitCountC2(int labelId, int countValue) async {
    try {
      return await apiService.registerCountC2(labelId, countValue);
    } catch (e) {
      throw Exception('Error al registrar conteo C2: $e');
    }
  }
}
```

---

## Modelos de Datos

### **`lib/models/marbete_model.dart`** - Entidad Marbete

```dart
import 'package:freezed_annotation/freezed_annotation.dart';

part 'marbete_model.freezed.dart';
part 'marbete_model.g.dart';

@freezed
class MarbeteModel with _$MarbeteModel {
  const factory MarbeteModel({
    required int id,
    required int numeroMarbete,
    required String producto,
    required String clave,
    required int stockTeorico,
    required String almacen,
    required String qrData,
    required DateTime fechaCreacion,
    int? countC1,
    int? countC2,
    required String estado, // ACTIVO, CONTABILIZADO, CANCELADO
  }) = _MarbeteModel;

  factory MarbeteModel.fromJson(Map<String, dynamic> json) =>
      _$MarbeteModelFromJson(json);
}
```

### **`lib/models/user_model.dart`** - Entidad Usuario

```dart
import 'package:freezed_annotation/freezed_annotation.dart';

part 'user_model.freezed.dart';
part 'user_model.g.dart';

@freezed
class UserModel with _$UserModel {
  const factory UserModel({
    required int id,
    required String email,
    required String role,
    required String almacen,
    required String nombreCompleto,
  }) = _UserModel;

  factory UserModel.fromJson(Map<String, dynamic> json) =>
      _$UserModelFromJson(json);
}
```

---

## Pantallas Principales

### **`lib/screens/marbete/marbete_detail_screen.dart`** - Detalles del Marbete

```dart
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../providers/marbete_provider.dart';
import '../../models/marbete_model.dart';

class MarbeteDetailScreen extends ConsumerWidget {
  final int numeroMarbete;

  const MarbeteDetailScreen({
    Key? key,
    required this.numeroMarbete,
  }) : super(key: key);

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final marbeteState = ref.watch(marbeteProvider);

    return Scaffold(
      appBar: AppBar(
        title: Text('Marbete #$numeroMarbete'),
      ),
      body: marbeteState.when(
        data: (marbete) => _buildMarbeteDetails(context, ref, marbete),
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (error, stack) => Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Icon(Icons.error, size: 48, color: Colors.red),
              const SizedBox(height: 16),
              Text(
                'Error: $error',
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 16),
              ElevatedButton(
                onPressed: () {
                  ref.refresh(marbeteProvider);
                },
                child: const Text('Reintentar'),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildMarbeteDetails(
    BuildContext context,
    WidgetRef ref,
    MarbeteModel marbete,
  ) {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Card con información principal
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  _buildInfoRow('Producto:', marbete.producto),
                  const Divider(),
                  _buildInfoRow('Clave:', marbete.clave),
                  const Divider(),
                  _buildInfoRow('Almacén:', marbete.almacen),
                  const Divider(),
                  _buildInfoRow('Stock Teórico:', '${marbete.stockTeorico} unidades'),
                  const Divider(),
                  _buildInfoRow('Estado:', marbete.estado),
                ],
              ),
            ),
          ),
          const SizedBox(height: 20),

          // Botones de acción
          if (marbete.estado == 'ACTIVO')
            Column(
              children: [
                ElevatedButton.icon(
                  icon: const Icon(Icons.check_circle),
                  label: const Text('Registrar Conteo C1'),
                  onPressed: () {
                    Navigator.of(context).pushNamed(
                      '/count-c1',
                      arguments: marbete,
                    );
                  },
                ),
                const SizedBox(height: 8),
                if (marbete.countC1 != null)
                  ElevatedButton.icon(
                    icon: const Icon(Icons.check_circle_outline),
                    label: const Text('Registrar Conteo C2'),
                    onPressed: () {
                      Navigator.of(context).pushNamed(
                        '/count-c2',
                        arguments: marbete,
                      );
                    },
                  ),
              ],
            ),
        ],
      ),
    );
  }

  Widget _buildInfoRow(String label, String value) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(
          label,
          style: const TextStyle(fontWeight: FontWeight.bold),
        ),
        Text(value),
      ],
    );
  }
}
```

### **`lib/screens/marbete/count_c1_screen.dart`** - Registrar Conteo C1

```dart
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../models/marbete_model.dart';
import '../../providers/marbete_provider.dart';

class CountC1Screen extends ConsumerStatefulWidget {
  final MarbeteModel marbete;

  const CountC1Screen({
    Key? key,
    required this.marbete,
  }) : super(key: key);

  @override
  ConsumerState<CountC1Screen> createState() => _CountC1ScreenState();
}

class _CountC1ScreenState extends ConsumerState<CountC1Screen> {
  late TextEditingController _countController;

  @override
  void initState() {
    super.initState();
    _countController = TextEditingController();
  }

  @override
  void dispose() {
    _countController.dispose();
    super.dispose();
  }

  void _submitCount() async {
    final countValue = int.tryParse(_countController.text);

    if (countValue == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Ingresa un número válido')),
      );
      return;
    }

    try {
      await ref.read(marbeteProvider.notifier).submitCountC1(
        widget.marbete.id,
        countValue,
      );

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Conteo C1 registrado exitosamente')),
        );
        Navigator.of(context).pop();
      }
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Error: $e')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('Conteo C1 - Marbete #${widget.marbete.numeroMarbete}'),
      ),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Producto: ${widget.marbete.producto}',
              style: Theme.of(context).textTheme.titleLarge,
            ),
            const SizedBox(height: 8),
            Text(
              'Stock Teórico: ${widget.marbete.stockTeorico} unidades',
              style: Theme.of(context).textTheme.bodyLarge,
            ),
            const SizedBox(height: 24),
            TextField(
              controller: _countController,
              keyboardType: TextInputType.number,
              decoration: InputDecoration(
                labelText: 'Cantidad física contada',
                hintText: '0',
                border: OutlineInputBorder(),
                suffixText: 'unidades',
              ),
            ),
            const SizedBox(height: 24),
            ElevatedButton.icon(
              icon: const Icon(Icons.check),
              label: const Text('Guardar Conteo C1'),
              onPressed: _submitCount,
            ),
          ],
        ),
      ),
    );
  }
}
```

---

## Ejemplos de Uso

### 1. **Login y Autenticación** - `lib/screens/auth/login_screen.dart`

```dart
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../providers/auth_provider.dart';

class LoginScreen extends ConsumerStatefulWidget {
  const LoginScreen({Key? key}) : super(key: key);

  @override
  ConsumerState<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends ConsumerState<LoginScreen> {
  late TextEditingController emailController;
  late TextEditingController passwordController;

  @override
  void initState() {
    super.initState();
    emailController = TextEditingController();
    passwordController = TextEditingController();
  }

  @override
  void dispose() {
    emailController.dispose();
    passwordController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('SIGMAV2 - Login')),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            TextField(
              controller: emailController,
              decoration: const InputDecoration(
                labelText: 'Email',
                hintText: 'usuario@tokai.com',
              ),
            ),
            const SizedBox(height: 16),
            TextField(
              controller: passwordController,
              obscureText: true,
              decoration: const InputDecoration(
                labelText: 'Contraseña',
              ),
            ),
            const SizedBox(height: 24),
            ElevatedButton(
              onPressed: () async {
                await ref.read(authProvider.notifier).login(
                  emailController.text,
                  passwordController.text,
                );
                if (mounted) {
                  Navigator.of(context).pushReplacementNamed('/home');
                }
              },
              child: const Text('Iniciar Sesión'),
            ),
          ],
        ),
      ),
    );
  }
}
```

---

## Testing Manual

### 1. **Verificar Conexión a API**

```bash
# En PowerShell, verifica que tu backend esté corriendo
curl -X GET http://localhost:8080/api/sigmav2/labels/1 `
  -H "Authorization: Bearer <TU_JWT_TOKEN>"
```

### 2. **Ejecutar Aplicación Flutter**

```bash
# En la carpeta del proyecto
flutter run

# O en un dispositivo específico
flutter run -d <device-id>

# Ver logs
flutter logs
```

### 3. **Test de Escaneo**

- Generar QR de prueba con el número del marbete (ej: "42")
- Apuntar cámara al QR desde la pantalla de scanner
- Verificar que redirija a detalles del marbete

---

## Consideraciones Finales

| Aspecto | Recomendación |
|--------|-----------|
| **Seguridad JWT** | Guardar token en `SharedPreferences` con expiración |
| **Manejo offline** | Implementar caché local con Hive |
| **Performance** | Usar `const` en widgets, implementar lazy loading |
| **UX/UI** | Material Design 3, tema claro/oscuro |
| **Logging** | Implementar logger para debugging en producción |
| **Testing** | Tests unitarios en `test/` |

---

## Próximos Pasos

1. ✅ Clonar este MD y crear carpetas según estructura
2. ✅ Ejecutar `flutter create sigmav2_mobile_app`
3. ✅ Actualizar `pubspec.yaml` con dependencias
4. ✅ Crear archivos de config (`.env`, `app_config.dart`)
5. ✅ Implementar `ApiService` y `QRScannerService`
6. ✅ Crear Providers con Riverpod
7. ✅ Implementar pantallas de Login → Scanner → Conteos
8. ✅ Testear flujo completo

---

**Versión:** 1.0.0  
**Fecha:** 2026-03-24  
**Desenvolvedor:** Cesar Uriel Gonzalez Saldaña - Tokai de México

