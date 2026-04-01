# 📱 IMPLEMENTACIÓN FLUTTER: QR Scanner + API Integration

**Fecha:** 23 de Marzo 2026  
**Stack:** Flutter (Dart) + Dio (HTTP) + GetX (State Management)  
**APIs:** SIGMAV2 REST Backend

---

## 📋 ÍNDICE

1. [Setup Inicial](#1-setup-inicial)
2. [Estructura del Proyecto](#2-estructura-del-proyecto)
3. [Servicios HTTP](#3-servicios-http)
4. [Modelos de Datos](#4-modelos-de-datos)
5. [Pantallas Principales](#5-pantallas-principales)
6. [Flujo Completo](#6-flujo-completo-paso-a-paso)

---

## 1. SETUP INICIAL

### 1.1 Dependencias (pubspec.yaml)

```yaml
dependencies:
  flutter:
    sdk: flutter
  
  # HTTP Client
  dio: ^5.3.1
  
  # State Management
  get: ^4.6.5
  
  # QR Scanner
  mobile_scanner: ^3.5.0
  
  # Secure Storage
  flutter_secure_storage: ^9.0.0
  
  # JSON Serialization
  json_annotation: ^4.8.1
  
  # Utilities
  uuid: ^4.0.0
  intl: ^0.19.0
  
dev_dependencies:
  build_runner: ^2.4.4
  json_serializable: ^6.7.0
```

### 1.2 Permisos (AndroidManifest.xml)

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.INTERNET" />
```

### 1.3 Permisos (Info.plist para iOS)

```xml
<key>NSCameraUsageDescription</key>
<string>La app necesita acceso a tu cámara para escanear códigos QR</string>
<key>NSLocalNetworkUsageDescription</key>
<string>La app necesita acceso a la red local</string>
```

---

## 2. ESTRUCTURA DEL PROYECTO

```
sigmav2_mobile/
├── lib/
│   ├── main.dart
│   ├── config/
│   │   ├── api_config.dart          # URLs y configuración
│   │   └── constants.dart           # Constantes globales
│   ├── models/
│   │   ├── auth_model.dart
│   │   ├── label_model.dart
│   │   ├── count_response_model.dart
│   │   └── warehouse_model.dart
│   ├── services/
│   │   ├── api_service.dart         # HTTP client (Dio)
│   │   ├── auth_service.dart
│   │   ├── label_service.dart
│   │   └── storage_service.dart     # Local storage para JWT
│   ├── controllers/
│   │   ├── auth_controller.dart
│   │   ├── label_controller.dart
│   │   └── warehouse_controller.dart
│   └── views/
│       ├── login_screen.dart
│       ├── home_screen.dart
│       ├── scanner_screen.dart
│       ├── validation_screen.dart
│       ├── count_screen.dart
│       ├── confirmation_screen.dart
│       └── widgets/
│           ├── custom_app_bar.dart
│           └── loading_widget.dart
```

---

## 3. SERVICIOS HTTP

### 3.1 ApiService.dart (Cliente HTTP con Dio)

```dart
import 'package:dio/dio.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'config/api_config.dart';

class ApiService {
  late Dio _dio;
  final _storage = const FlutterSecureStorage();
  
  static final ApiService _instance = ApiService._internal();
  
  ApiService._internal() {
    _initializeDio();
  }
  
  factory ApiService() {
    return _instance;
  }
  
  void _initializeDio() {
    _dio = Dio(
      BaseOptions(
        baseUrl: ApiConfig.baseUrl,
        connectTimeout: const Duration(seconds: 10),
        receiveTimeout: const Duration(seconds: 10),
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json',
        },
      ),
    );
    
    // Interceptor: Agregar JWT Token
    _dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) async {
          String? token = await _storage.read(key: 'jwt_token');
          if (token != null) {
            options.headers['Authorization'] = 'Bearer $token';
          }
          return handler.next(options);
        },
        onResponse: (response, handler) {
          return handler.next(response);
        },
        onError: (error, handler) {
          // Manejar 401 Unauthorized
          if (error.response?.statusCode == 401) {
            // Token expirado → redirigir a login
          }
          return handler.next(error);
        },
      ),
    );
  }
  
  Future<T> get<T>(
    String path, {
    Map<String, dynamic>? queryParameters,
    required T Function(dynamic) converter,
  }) async {
    try {
      final response = await _dio.get(
        path,
        queryParameters: queryParameters,
      );
      return converter(response.data);
    } catch (e) {
      rethrow;
    }
  }
  
  Future<T> post<T>(
    String path, {
    required Map<String, dynamic> data,
    required T Function(dynamic) converter,
  }) async {
    try {
      final response = await _dio.post(path, data: data);
      return converter(response.data);
    } catch (e) {
      rethrow;
    }
  }
  
  // Método específico para guardar token
  Future<void> saveToken(String token) async {
    await _storage.write(key: 'jwt_token', value: token);
  }
  
  // Método para obtener token
  Future<String?> getToken() async {
    return await _storage.read(key: 'jwt_token');
  }
  
  // Método para eliminar token (logout)
  Future<void> deleteToken() async {
    await _storage.delete(key: 'jwt_token');
  }
}
```

### 3.2 LabelService.dart

```dart
import 'package:dio/dio.dart';
import 'models/label_model.dart';
import 'models/count_response_model.dart';

class LabelService {
  final ApiService _apiService = ApiService();
  
  // 1. VALIDAR QR
  Future<LabelValidationResponse> validateLabel({
    required String qrCode,
    required String countType,
    required int warehouseId,
    required int periodId,
  }) async {
    try {
      final response = await _apiService.post(
        '/labels/scan/validate',
        data: {
          'qrCode': qrCode,
          'countType': countType,
          'warehouseId': warehouseId,
          'periodId': periodId,
        },
        converter: (data) => LabelValidationResponse.fromJson(data),
      );
      return response;
    } catch (e) {
      throw ApiException('Error validando etiqueta: $e');
    }
  }
  
  // 2. REGISTRAR CONTEO
  Future<LabelCountResponse> registerCount({
    required int folio,
    required String countType,
    required int quantity,
    required int warehouseId,
    required int periodId,
    required String deviceId,
  }) async {
    try {
      final response = await _apiService.post(
        '/labels/scan/count',
        data: {
          'folio': folio,
          'countType': countType,
          'quantity': quantity,
          'warehouseId': warehouseId,
          'periodId': periodId,
          'deviceId': deviceId,
          'scanTimestamp': DateTime.now().toIso8601String(),
        },
        converter: (data) => LabelCountResponse.fromJson(data),
      );
      return response;
    } catch (e) {
      throw ApiException('Error registrando conteo: $e');
    }
  }
  
  // 3. OBTENER ESTADO
  Future<LabelStatusResponse> getStatus({
    required int folio,
    required int warehouseId,
    required int periodId,
  }) async {
    try {
      final response = await _apiService.get(
        '/labels/scan/status/$folio',
        queryParameters: {
          'warehouseId': warehouseId,
          'periodId': periodId,
        },
        converter: (data) => LabelStatusResponse.fromJson(data),
      );
      return response;
    } catch (e) {
      throw ApiException('Error obteniendo estado: $e');
    }
  }
  
  // 4. BUSCAR POR FOLIO
  Future<LabelValidationResponse> findByFolio({
    required int folio,
    required int warehouseId,
    required int periodId,
    required String countType,
  }) async {
    try {
      final response = await _apiService.get(
        '/labels/scan/folio/$folio',
        queryParameters: {
          'warehouseId': warehouseId,
          'periodId': periodId,
          'countType': countType,
        },
        converter: (data) => LabelValidationResponse.fromJson(data),
      );
      return response;
    } catch (e) {
      throw ApiException('Error buscando folio: $e');
    }
  }
  
  // 5. OBTENER PENDIENTES
  Future<PendingLabelsResponse> getPendingLabels({
    required int warehouseId,
    required int periodId,
    required String countType,
    int limit = 20,
  }) async {
    try {
      final response = await _apiService.get(
        '/labels/scan/pending',
        queryParameters: {
          'warehouseId': warehouseId,
          'periodId': periodId,
          'countType': countType,
          'limit': limit,
        },
        converter: (data) => PendingLabelsResponse.fromJson(data),
      );
      return response;
    } catch (e) {
      throw ApiException('Error obteniendo pendientes: $e');
    }
  }
}

class ApiException implements Exception {
  final String message;
  ApiException(this.message);
  
  @override
  String toString() => message;
}
```

### 3.3 AuthService.dart

```dart
import 'models/auth_model.dart';

class AuthService {
  final ApiService _apiService = ApiService();
  
  Future<AuthResponse> login({
    required String email,
    required String password,
  }) async {
    try {
      final response = await _apiService.post(
        '/auth/login',
        data: {
          'email': email,
          'password': password,
        },
        converter: (data) => AuthResponse.fromJson(data),
      );
      
      // Guardar token
      await _apiService.saveToken(response.token);
      
      return response;
    } catch (e) {
      throw ApiException('Error en login: $e');
    }
  }
  
  Future<void> logout() async {
    await _apiService.deleteToken();
  }
  
  Future<bool> isLoggedIn() async {
    final token = await _apiService.getToken();
    return token != null;
  }
}
```

---

## 4. MODELOS DE DATOS

### 4.1 label_model.dart

```dart
import 'package:json_annotation/json_annotation.dart';

part 'label_model.g.dart';

@JsonSerializable()
class LabelValidationResponse {
  final bool valid;
  final int folio;
  final String qrCode;
  final int productId;
  final String productName;
  final int theoreticalQuantity;
  final String estado;
  final CountInfo? c1;
  final CountInfo? c2;
  final String message;
  final String validationStatus;
  final String? error;
  
  LabelValidationResponse({
    required this.valid,
    required this.folio,
    required this.qrCode,
    required this.productId,
    required this.productName,
    required this.theoreticalQuantity,
    required this.estado,
    this.c1,
    this.c2,
    required this.message,
    required this.validationStatus,
    this.error,
  });
  
  factory LabelValidationResponse.fromJson(Map<String, dynamic> json) =>
      _$LabelValidationResponseFromJson(json);
  
  Map<String, dynamic> toJson() => _$LabelValidationResponseToJson(this);
}

@JsonSerializable()
class CountInfo {
  final bool registered;
  final int? quantity;
  @JsonKey(name: 'registeredAt')
  final DateTime? registeredAt;
  
  CountInfo({
    required this.registered,
    this.quantity,
    this.registeredAt,
  });
  
  factory CountInfo.fromJson(Map<String, dynamic> json) =>
      _$CountInfoFromJson(json);
  
  Map<String, dynamic> toJson() => _$CountInfoToJson(this);
}

@JsonSerializable()
class LabelStatusResponse {
  final int folio;
  final String qrCode;
  final String productName;
  final String estado;
  final int theoretical;
  final CountInfo? c1;
  final CountInfo? c2;
  final int? variance;
  final bool readyForC2;
  final String message;
  
  LabelStatusResponse({
    required this.folio,
    required this.qrCode,
    required this.productName,
    required this.estado,
    required this.theoretical,
    this.c1,
    this.c2,
    this.variance,
    required this.readyForC2,
    required this.message,
  });
  
  factory LabelStatusResponse.fromJson(Map<String, dynamic> json) =>
      _$LabelStatusResponseFromJson(json);
  
  Map<String, dynamic> toJson() => _$LabelStatusResponseToJson(this);
}
```

### 4.2 count_response_model.dart

```dart
import 'package:json_annotation/json_annotation.dart';

part 'count_response_model.g.dart';

@JsonSerializable()
class LabelCountResponse {
  final bool success;
  final int folio;
  final String countType;
  final int quantity;
  @JsonKey(name: 'registeredAt')
  final DateTime registeredAt;
  final int? variance;
  final String message;
  
  LabelCountResponse({
    required this.success,
    required this.folio,
    required this.countType,
    required this.quantity,
    required this.registeredAt,
    this.variance,
    required this.message,
  });
  
  factory LabelCountResponse.fromJson(Map<String, dynamic> json) =>
      _$LabelCountResponseFromJson(json);
  
  Map<String, dynamic> toJson() => _$LabelCountResponseToJson(this);
}

@JsonSerializable()
class PendingLabelsResponse {
  final int total;
  final int pending;
  final int completed;
  final List<PendingLabel> labels;
  
  PendingLabelsResponse({
    required this.total,
    required this.pending,
    required this.completed,
    required this.labels,
  });
  
  factory PendingLabelsResponse.fromJson(Map<String, dynamic> json) =>
      _$PendingLabelsResponseFromJson(json);
  
  Map<String, dynamic> toJson() => _$PendingLabelsResponseToJson(this);
}

@JsonSerializable()
class PendingLabel {
  final int folio;
  final String qrCode;
  final String productName;
  final int theoretical;
  final int? c1;
  final int? c2;
  @JsonKey(name: 'lastScannedAt')
  final DateTime? lastScannedAt;
  
  PendingLabel({
    required this.folio,
    required this.qrCode,
    required this.productName,
    required this.theoretical,
    this.c1,
    this.c2,
    this.lastScannedAt,
  });
  
  factory PendingLabel.fromJson(Map<String, dynamic> json) =>
      _$PendingLabelFromJson(json);
  
  Map<String, dynamic> toJson() => _$PendingLabelToJson(this);
}
```

---

## 5. PANTALLAS PRINCIPALES

### 5.1 LoginScreen.dart

```dart
import 'package:flutter/material.dart';
import 'package:get/get.dart';
import '../controllers/auth_controller.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({Key? key}) : super(key: key);
  
  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final AuthController authController = Get.put(AuthController());
  final TextEditingController emailController = TextEditingController();
  final TextEditingController passwordController = TextEditingController();
  
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('SIGMAV2 - Login'),
        backgroundColor: Colors.blue[900],
      ),
      body: SingleChildScrollView(
        child: Padding(
          padding: const EdgeInsets.all(20.0),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const SizedBox(height: 60),
              // Logo
              Icon(
                Icons.inventory_2,
                size: 80,
                color: Colors.blue[900],
              ),
              const SizedBox(height: 30),
              // Email
              TextField(
                controller: emailController,
                decoration: InputDecoration(
                  labelText: 'Email',
                  prefixIcon: const Icon(Icons.email),
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(10),
                  ),
                ),
                keyboardType: TextInputType.emailAddress,
              ),
              const SizedBox(height: 20),
              // Password
              TextField(
                controller: passwordController,
                decoration: InputDecoration(
                  labelText: 'Contraseña',
                  prefixIcon: const Icon(Icons.lock),
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(10),
                  ),
                ),
                obscureText: true,
              ),
              const SizedBox(height: 30),
              // Login Button
              Obx(
                () => ElevatedButton(
                  onPressed: authController.isLoading.value
                      ? null
                      : () {
                          authController.login(
                            email: emailController.text,
                            password: passwordController.text,
                          );
                        },
                  style: ElevatedButton.styleFrom(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 50,
                      vertical: 15,
                    ),
                    backgroundColor: Colors.blue[900],
                  ),
                  child: authController.isLoading.value
                      ? const SizedBox(
                          height: 20,
                          width: 20,
                          child: CircularProgressIndicator(
                            valueColor:
                                AlwaysStoppedAnimation<Color>(Colors.white),
                          ),
                        )
                      : const Text(
                          'Ingresar',
                          style: TextStyle(fontSize: 16, color: Colors.white),
                        ),
                ),
              ),
              const SizedBox(height: 20),
              // Error Message
              Obx(
                () => authController.errorMessage.value.isNotEmpty
                    ? Container(
                        padding: const EdgeInsets.all(15),
                        decoration: BoxDecoration(
                          color: Colors.red[100],
                          borderRadius: BorderRadius.circular(10),
                          border: Border.all(color: Colors.red),
                        ),
                        child: Text(
                          authController.errorMessage.value,
                          style: TextStyle(color: Colors.red[900]),
                        ),
                      )
                    : const SizedBox.shrink(),
              ),
            ],
          ),
        ),
      ),
    );
  }
  
  @override
  void dispose() {
    emailController.dispose();
    passwordController.dispose();
    super.dispose();
  }
}
```

### 5.2 ScannerScreen.dart (QR Scanner)

```dart
import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:mobile_scanner/mobile_scanner.dart';
import '../controllers/label_controller.dart';

class ScannerScreen extends StatefulWidget {
  final int warehouseId;
  final int periodId;
  final String countType;  // C1 o C2
  
  const ScannerScreen({
    required this.warehouseId,
    required this.periodId,
    required this.countType,
    Key? key,
  }) : super(key: key);
  
  @override
  State<ScannerScreen> createState() => _ScannerScreenState();
}

class _ScannerScreenState extends State<ScannerScreen> {
  final LabelController labelController = Get.find();
  MobileScannerController cameraController = MobileScannerController();
  bool _isProcessing = false;
  
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Escanear Marbete'),
        backgroundColor: Colors.blue[900],
      ),
      body: Stack(
        children: [
          // Camera Scanner
          MobileScanner(
            controller: cameraController,
            onDetect: (capture) async {
              if (_isProcessing) return;
              
              final List<Barcode> barcodes = capture.barcodes;
              for (final barcode in barcodes) {
                final String? code = barcode.rawValue;
                if (code != null) {
                  _isProcessing = true;
                  
                  // Procesar QR
                  _handleQRDetected(code);
                  
                  break;
                }
              }
            },
          ),
          
          // Overlay UI
          Container(
            alignment: Alignment.bottomCenter,
            child: Padding(
              padding: const EdgeInsets.all(20.0),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.end,
                children: [
                  const Text(
                    'Apunta con la cámara al código QR',
                    style: TextStyle(
                      color: Colors.white,
                      fontSize: 16,
                      fontWeight: FontWeight.bold,
                      shadows: [
                        Shadow(
                          offset: Offset(1, 1),
                          blurRadius: 2,
                          color: Colors.black54,
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 30),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                    children: [
                      // Flip Camera
                      FloatingActionButton(
                        onPressed: () => cameraController.switchCamera(),
                        child: const Icon(Icons.flip_camera_android),
                      ),
                      // Manual Entry
                      FloatingActionButton.extended(
                        onPressed: _showManualEntry,
                        label: const Text('Ingreso Manual'),
                        icon: const Icon(Icons.keyboard),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
  
  void _handleQRDetected(String qrCode) async {
    try {
      // Validar marbete
      await labelController.validateLabel(
        qrCode: qrCode,
        countType: widget.countType,
        warehouseId: widget.warehouseId,
        periodId: widget.periodId,
      );
      
      if (labelController.validationResponse.value != null &&
          labelController.validationResponse.value!.valid) {
        // Ir a pantalla de validación
        Get.to(
          () => ValidationScreen(
            validation: labelController.validationResponse.value!,
            warehouseId: widget.warehouseId,
            periodId: widget.periodId,
            countType: widget.countType,
          ),
        );
      } else {
        // Mostrar error
        _showError(
            labelController.validationResponse.value?.message ??
                'Marbete inválido',
            context);
      }
    } catch (e) {
      _showError('Error: $e', context);
    } finally {
      _isProcessing = false;
    }
  }
  
  void _showManualEntry() {
    final TextEditingController folioController = TextEditingController();
    
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Ingreso Manual de Folio'),
        content: TextField(
          controller: folioController,
          keyboardType: TextInputType.number,
          decoration: const InputDecoration(
            labelText: 'Número de Folio',
            hintText: 'Ej: 42',
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Cancelar'),
          ),
          ElevatedButton(
            onPressed: () {
              _handleQRDetected(folioController.text);
              Navigator.pop(context);
            },
            child: const Text('Buscar'),
          ),
        ],
      ),
    );
  }
  
  void _showError(String message, BuildContext context) {
    _isProcessing = false;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: Colors.red,
      ),
    );
  }
  
  @override
  void dispose() {
    cameraController.dispose();
    super.dispose();
  }
}
```

### 5.3 ValidationScreen.dart

```dart
import 'package:flutter/material.dart';
import 'package:get/get.dart';
import '../models/label_model.dart';
import '../controllers/label_controller.dart';
import 'count_screen.dart';

class ValidationScreen extends StatelessWidget {
  final LabelValidationResponse validation;
  final int warehouseId;
  final int periodId;
  final String countType;
  
  const ValidationScreen({
    required this.validation,
    required this.warehouseId,
    required this.periodId,
    required this.countType,
    Key? key,
  }) : super(key: key);
  
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('Marbete #${validation.folio}'),
        backgroundColor: Colors.blue[900],
      ),
      body: Padding(
        padding: const EdgeInsets.all(20.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Status Card
            Card(
              color: Colors.green[50],
              child: Padding(
                padding: const EdgeInsets.all(15.0),
                child: Row(
                  children: [
                    Icon(
                      Icons.check_circle,
                      color: Colors.green[700],
                      size: 40,
                    ),
                    const SizedBox(width: 15),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Text(
                            'MARBETE VÁLIDO',
                            style: TextStyle(
                              fontWeight: FontWeight.bold,
                              fontSize: 16,
                            ),
                          ),
                          Text(
                            validation.message,
                            style: TextStyle(color: Colors.grey[700]),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 30),
            
            // Product Info
            const Text(
              'Información del Producto',
              style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
            ),
            const SizedBox(height: 10),
            _buildInfoRow('Nombre', validation.productName),
            _buildInfoRow('Teórico', '${validation.theoreticalQuantity} unidades'),
            _buildInfoRow('Almacén', warehouseId.toString()),
            const SizedBox(height: 20),
            
            // Count Status
            const Text(
              'Estado de Conteos',
              style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
            ),
            const SizedBox(height: 10),
            _buildCountStatus('C1', validation.c1),
            _buildCountStatus('C2', validation.c2),
            const SizedBox(height: 30),
            
            // Continue Button
            SizedBox(
              width: double.infinity,
              height: 50,
              child: ElevatedButton(
                onPressed: () {
                  Get.to(
                    () => CountScreen(
                      folio: validation.folio,
                      productName: validation.productName,
                      warehouseId: warehouseId,
                      periodId: periodId,
                      countType: countType,
                      theoretical: validation.theoreticalQuantity,
                    ),
                  );
                },
                style: ElevatedButton.styleFrom(
                  backgroundColor: Colors.blue[900],
                ),
                child: const Text(
                  'Continuar con el Conteo',
                  style: TextStyle(color: Colors.white, fontSize: 16),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
  
  Widget _buildInfoRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8.0),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label, style: const TextStyle(color: Colors.grey)),
          Text(value, style: const TextStyle(fontWeight: FontWeight.bold)),
        ],
      ),
    );
  }
  
  Widget _buildCountStatus(String countType, CountInfo? countInfo) {
    bool isRegistered = countInfo?.registered ?? false;
    
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8.0),
      child: Row(
        children: [
          Expanded(
            child: Text(countType),
          ),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
            decoration: BoxDecoration(
              color: isRegistered ? Colors.amber[100] : Colors.grey[200],
              borderRadius: BorderRadius.circular(20),
            ),
            child: Text(
              isRegistered
                  ? '✓ ${countInfo!.quantity} unidades'
                  : '⏳ Pendiente',
              style: TextStyle(
                fontSize: 12,
                color: isRegistered ? Colors.amber[900] : Colors.grey[700],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
```

### 5.4 CountScreen.dart

```dart
import 'package:flutter/material.dart';
import 'package:get/get.dart';
import '../controllers/label_controller.dart';
import 'confirmation_screen.dart';

class CountScreen extends StatefulWidget {
  final int folio;
  final String productName;
  final int warehouseId;
  final int periodId;
  final String countType;
  final int theoretical;
  
  const CountScreen({
    required this.folio,
    required this.productName,
    required this.warehouseId,
    required this.periodId,
    required this.countType,
    required this.theoretical,
    Key? key,
  }) : super(key: key);
  
  @override
  State<CountScreen> createState() => _CountScreenState();
}

class _CountScreenState extends State<CountScreen> {
  final LabelController labelController = Get.find();
  final TextEditingController quantityController = TextEditingController();
  
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Registrar Conteo'),
        backgroundColor: Colors.blue[900],
      ),
      body: Padding(
        padding: const EdgeInsets.all(20.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Product Header
            Card(
              child: Padding(
                padding: const EdgeInsets.all(15.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      widget.productName,
                      style: const TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(height: 10),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            const Text(
                              'Marbete',
                              style: TextStyle(color: Colors.grey),
                            ),
                            Text(
                              '#${widget.folio}',
                              style: const TextStyle(
                                fontSize: 16,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                          ],
                        ),
                        Column(
                          crossAxisAlignment: CrossAxisAlignment.end,
                          children: [
                            const Text(
                              'Teórico',
                              style: TextStyle(color: Colors.grey),
                            ),
                            Text(
                              '${widget.theoretical} unidades',
                              style: const TextStyle(
                                fontSize: 16,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                          ],
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 30),
            
            // Count Type
            Container(
              padding: const EdgeInsets.all(15),
              decoration: BoxDecoration(
                color: Colors.blue[50],
                borderRadius: BorderRadius.circular(10),
              ),
              child: Row(
                children: [
                  Icon(
                    Icons.info,
                    color: Colors.blue[900],
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Text(
                      'Conteo: ${widget.countType} (${widget.countType == 'C1' ? 'Primer Conteo' : 'Segundo Conteo'})',
                      style: TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.bold,
                        color: Colors.blue[900],
                      ),
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 40),
            
            // Quantity Input
            const Text(
              'Cantidad Contada',
              style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 15),
            TextField(
              controller: quantityController,
              keyboardType: TextInputType.number,
              textAlign: TextAlign.center,
              style: const TextStyle(fontSize: 32, fontWeight: FontWeight.bold),
              decoration: InputDecoration(
                hintText: '0',
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(10),
                ),
                contentPadding: const EdgeInsets.symmetric(vertical: 20),
              ),
            ),
            const SizedBox(height: 20),
            
            // Quick Add Buttons
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
              children: [
                _buildQuickAddButton('-10', () {
                  int current = int.tryParse(quantityController.text) ?? 0;
                  quantityController.text = (current - 10).clamp(0, 999999).toString();
                }),
                _buildQuickAddButton('+10', () {
                  int current = int.tryParse(quantityController.text) ?? 0;
                  quantityController.text = (current + 10).clamp(0, 999999).toString();
                }),
                _buildQuickAddButton('+100', () {
                  int current = int.tryParse(quantityController.text) ?? 0;
                  quantityController.text = (current + 100).clamp(0, 999999).toString();
                }),
              ],
            ),
            const Spacer(),
            
            // Save Button
            Obx(
              () => SizedBox(
                width: double.infinity,
                height: 50,
                child: ElevatedButton(
                  onPressed: labelController.isLoading.value
                      ? null
                      : () => _registerCount(),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: Colors.green[700],
                  ),
                  child: labelController.isLoading.value
                      ? const SizedBox(
                          height: 20,
                          width: 20,
                          child: CircularProgressIndicator(
                            valueColor:
                                AlwaysStoppedAnimation<Color>(Colors.white),
                          ),
                        )
                      : const Text(
                          '✓ Guardar Conteo',
                          style: TextStyle(
                            color: Colors.white,
                            fontSize: 16,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                ),
              ),
            ),
            const SizedBox(height: 10),
            SizedBox(
              width: double.infinity,
              height: 50,
              child: OutlinedButton(
                onPressed: () => Get.back(),
                child: const Text('Cancelar'),
              ),
            ),
          ],
        ),
      ),
    );
  }
  
  void _registerCount() async {
    int? quantity = int.tryParse(quantityController.text);
    
    if (quantity == null || quantity <= 0) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Ingresa una cantidad válida'),
          backgroundColor: Colors.red,
        ),
      );
      return;
    }
    
    try {
      await labelController.registerCount(
        folio: widget.folio,
        countType: widget.countType,
        quantity: quantity,
        warehouseId: widget.warehouseId,
        periodId: widget.periodId,
      );
      
      // Ir a confirmación
      Get.to(
        () => ConfirmationScreen(
          countResponse: labelController.countResponse.value!,
          productName: widget.productName,
          countType: widget.countType,
        ),
      );
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Error: $e'),
          backgroundColor: Colors.red,
        ),
      );
    }
  }
  
  Widget _buildQuickAddButton(String label, VoidCallback onPressed) {
    return ElevatedButton(
      onPressed: onPressed,
      style: ElevatedButton.styleFrom(
        backgroundColor: Colors.grey[300],
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
      ),
      child: Text(
        label,
        style: const TextStyle(
          color: Colors.black,
          fontWeight: FontWeight.bold,
        ),
      ),
    );
  }
  
  @override
  void dispose() {
    quantityController.dispose();
    super.dispose();
  }
}
```

### 5.5 ConfirmationScreen.dart

```dart
import 'package:flutter/material.dart';
import 'package:get/get.dart';
import '../models/count_response_model.dart';
import 'home_screen.dart';

class ConfirmationScreen extends StatelessWidget {
  final LabelCountResponse countResponse;
  final String productName;
  final String countType;
  
  const ConfirmationScreen({
    required this.countResponse,
    required this.productName,
    required this.countType,
    Key? key,
  }) : super(key: key);
  
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Confirmación'),
        backgroundColor: Colors.blue[900],
        automaticallyImplyLeading: false,
      ),
      body: Padding(
        padding: const EdgeInsets.all(20.0),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            // Success Icon
            Container(
              width: 100,
              height: 100,
              decoration: BoxDecoration(
                color: Colors.green[100],
                shape: BoxShape.circle,
              ),
              child: Icon(
                Icons.check_circle,
                size: 60,
                color: Colors.green[700],
              ),
            ),
            const SizedBox(height: 30),
            
            // Success Message
            const Text(
              '✅ ÉXITO',
              style: TextStyle(
                fontSize: 28,
                fontWeight: FontWeight.bold,
                color: Colors.green,
              ),
            ),
            const SizedBox(height: 15),
            Text(
              countResponse.message,
              style: const TextStyle(fontSize: 16, color: Colors.grey),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 40),
            
            // Details Card
            Card(
              child: Padding(
                padding: const EdgeInsets.all(20.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    _buildDetailRow('Marbete', '#${countResponse.folio}'),
                    _buildDetailRow('Producto', productName),
                    _buildDetailRow('Tipo', countType),
                    _buildDetailRow('Cantidad', '${countResponse.quantity} unidades'),
                    if (countResponse.variance != null) ...[
                      const SizedBox(height: 10),
                      _buildDetailRow(
                        'Varianza',
                        '${countResponse.variance} unidades',
                        color: countResponse.variance == 0
                            ? Colors.green
                            : Colors.orange,
                      ),
                    ],
                    _buildDetailRow(
                      'Registrado',
                      _formatTime(countResponse.registeredAt),
                    ),
                  ],
                ),
              ),
            ),
            const Spacer(),
            
            // Action Buttons
            SizedBox(
              width: double.infinity,
              height: 50,
              child: ElevatedButton(
                onPressed: () => Get.offAll(() => const HomeScreen()),
                style: ElevatedButton.styleFrom(
                  backgroundColor: Colors.blue[900],
                ),
                child: const Text(
                  '🔄 Siguiente Marbete',
                  style: TextStyle(
                    color: Colors.white,
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ),
            ),
            const SizedBox(height: 10),
            SizedBox(
              width: double.infinity,
              height: 50,
              child: OutlinedButton(
                onPressed: () => Get.offAll(() => const HomeScreen()),
                child: const Text('📋 Ver Dashboard'),
              ),
            ),
          ],
        ),
      ),
    );
  }
  
  Widget _buildDetailRow(String label, String value, {Color? color}) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8.0),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label, style: const TextStyle(color: Colors.grey)),
          Text(
            value,
            style: TextStyle(
              fontWeight: FontWeight.bold,
              color: color,
            ),
          ),
        ],
      ),
    );
  }
  
  String _formatTime(DateTime dateTime) {
    return '${dateTime.hour}:${dateTime.minute.toString().padLeft(2, '0')}:${dateTime.second.toString().padLeft(2, '0')}';
  }
}
```

---

## 6. FLUJO COMPLETO PASO A PASO

```
1. USUARIO ABRE APP
   ├─> ¿JWT Token almacenado?
   ├─> NO → LoginScreen
   └─> SÍ → HomeScreen

2. LOGIN
   ├─> POST /auth/login
   ├─> Guardar token en Keychain/Keystore
   └─> Ir a HomeScreen

3. HOME SCREEN
   ├─> GET /warehouses
   ├─> GET /periods/active
   ├─> Mostrar almacenes + período
   └─> Usuario selecciona almacén + contType (C1/C2)

4. SCANNER
   ├─> Abrir cámara
   ├─> Escanear QR: "SIGMAV2-FOLIO-42-P16-W369"
   ├─> POST /labels/scan/validate
   └─> ✓ VALID → ValidationScreen

5. VALIDACIÓN
   ├─> Mostrar info del marbete
   ├─> Mostrar estado (C1/C2)
   └─> Usuario toca "Continuar"

6. CONTEO
   ├─> Mostrar pantalla de cantidad
   ├─> Usuario ingresa cantidad: 95
   └─> Usuario toca "Guardar Conteo"

7. REGISTRAR
   ├─> POST /labels/scan/count
   │   ├─> folio: 42
   │   ├─> quantity: 95
   │   ├─> countType: C1
   │   ├─> deviceId: UUID
   │   └─> scanTimestamp: NOW
   ├─> ✓ ÉXITO
   └─> ConfirmationScreen

8. CONFIRMACIÓN
   ├─> Mostrar resultado
   ├─> Mostrar varianza
   └─> [Siguiente] o [Dashboard]

9. SIGUIENTE
   └─> Volver a Scanner (paso 4)
```

---

**Generación de modelos (JSON Serializable):**

```bash
flutter pub run build_runner build
```

**Run app:**

```bash
flutter run
```

---

**Próximos pasos:**
- ✅ Implementar offline sync (Hive/SqLite)
- ✅ Agregar reportes en móvil
- ✅ Testing automation
- ✅ Push notifications


