# 📐 DIAGRAMA DE ARQUITECTURA - AULAVIVA

```
┌─────────────────────────────────────────────────────────────────┐
│                          AULAVIVA APP                            │
│                    MVVM + CLEAN ARCHITECTURE                     │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                      PRESENTATION LAYER                          │
│                         (UI + ViewModel)                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────────┐         ┌──────────────────┐             │
│  │  LoginActivity   │────────▶│  AuthViewModel   │             │
│  │  (ViewBinding)   │         │  (LiveData)      │             │
│  └──────────────────┘         └──────────────────┘             │
│           │                             │                        │
│           │                             ▼                        │
│           │                    ┌──────────────────┐             │
│           │                    │ AuthRepository   │             │
│           │                    └──────────────────┘             │
│           │                                                      │
│  ┌──────────────────┐         ┌──────────────────┐             │
│  │RegisterActivity  │────────▶│  AuthViewModel   │             │
│  │  (ViewBinding)   │         │  (LiveData)      │             │
│  └──────────────────┘         └──────────────────┘             │
│                                                                  │
│  ┌──────────────────┐         ┌──────────────────┐             │
│  │PanelPrincipal    │────────▶│  AuthViewModel   │             │
│  │Activity          │         │  (LiveData)      │             │
│  │ + LOGOUT ✅      │         └──────────────────┘             │
│  └──────────────────┘                                           │
│                                                                  │
│  ┌──────────────────┐         ┌──────────────────┐             │
│  │ListaClases       │────────▶│  ClaseViewModel  │             │
│  │Activity          │         │  (LiveData)      │             │
│  │ + ClaseAdapter   │         └──────────────────┘             │
│  └──────────────────┘                  │                        │
│                                        ▼                        │
│                               ┌──────────────────┐             │
│                               │ ClaseRepository  │             │
│                               └──────────────────┘             │
└─────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────┐
│                         DOMAIN LAYER                             │
│                     (Lógica de Negocio)                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│   ┌────────────────────┐        ┌────────────────────┐         │
│   │ AuthRepository     │        │ ClaseRepository    │         │
│   │                    │        │                    │         │
│   │ • login()          │        │ • obtenerClases()  │         │
│   │ • register()       │        │ • crearClase()     │         │
│   │ • logout()         │        │ • actualizarClase()│         │
│   │ • getCurrentUser() │        │ • eliminarClase()  │         │
│   └────────────────────┘        └────────────────────┘         │
│            │                              │                      │
│            └──────────────┬───────────────┘                     │
└───────────────────────────┼──────────────────────────────────────┘
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                          DATA LAYER                              │
│                    (Fuentes de Datos)                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│   ┌────────────────────────────────────────────────┐           │
│   │              FIREBASE SERVICES                  │           │
│   │                                                 │           │
│   │  ┌──────────────────┐  ┌──────────────────┐   │           │
│   │  │ Firebase Auth    │  │  Firestore DB    │   │           │
│   │  │                  │  │                  │   │           │
│   │  │ • Authentication │  │ • usuarios/      │   │           │
│   │  │ • User Sessions  │  │ • clases/        │   │           │
│   │  └──────────────────┘  └──────────────────┘   │           │
│   └────────────────────────────────────────────────┘           │
│                                                                  │
│   ┌────────────────────┐                                        │
│   │   DATA MODELS      │                                        │
│   │                    │                                        │
│   │   • Clase.kt       │                                        │
│   │   • (Usuario.kt)   │  ← Futuro                             │
│   └────────────────────┘                                        │
└─────────────────────────────────────────────────────────────────┘


═══════════════════════════════════════════════════════════════════
                        FLUJO DE DATOS
═══════════════════════════════════════════════════════════════════

EJEMPLO: Login de Usuario
──────────────────────────

1. Usuario presiona botón "Iniciar sesión"
   │
   ▼
2. LoginActivity.onClick()
   │
   ▼
3. viewModel.login(email, password)
   │
   ▼
4. AuthViewModel valida datos
   │
   ▼
5. AuthRepository.login()
   │
   ▼
6. Firebase Authentication
   │
   ▼
7. Repository responde: onSuccess() / onError()
   │
   ▼
8. ViewModel actualiza LiveData
   │
   ▼
9. Observer en Activity reacciona
   │
   ▼
10. UI se actualiza (navega a Panel Principal)


═══════════════════════════════════════════════════════════════════
                    ESTRUCTURA DE PAQUETES
═══════════════════════════════════════════════════════════════════

cl.duocuc.aulaviva
│
├── 📂 data
│   ├── 📂 model
│   │   └── Clase.kt
│   └── 📂 repository
│       ├── AuthRepository.kt
│       └── ClaseRepository.kt
│
├── 📂 presentation
│   ├── 📂 viewmodel
│   │   ├── AuthViewModel.kt
│   │   └── ClaseViewModel.kt
│   └── 📂 adapter
│       └── ClaseAdapter.kt
│
└── ���� Activities (en raíz)
    ├── LoginActivity.kt
    ├── RegisterActivity.kt
    ├── PanelPrincipalActivity.kt
    └── ListaClasesActivity.kt


═══════════════════════════════════════════════════════════════════
                    COMPONENTES CLAVE
═══════════════════════════════════════════════════════════════════

┌─────────────────────────────────────────────────────────────────┐
│ VIEWMODEL (presentation/viewmodel/)                             │
├─────────────────────────────────────────────────────────────────┤
│ • Extiende de androidx.lifecycle.ViewModel                      │
│ • Contiene LiveData para observar estados                       │
│ • NO tiene referencias a Context ni Views                       │
│ • Sobrevive a cambios de configuración                          │
│ • Se comunica con Repository                                    │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│ REPOSITORY (data/repository/)                                   │
├─────────────────────────────────────────────────────────────────┤
│ • Única fuente de verdad para datos                            │
│ • Abstrae el origen de datos (Firebase, DB local, API)         │
│ • Usa callbacks para comunicarse con ViewModel                  │
│ • Maneja la lógica de acceso a datos                           │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│ ACTIVITY (raíz del paquete)                                     │
├─────────────────────────────────────────────────────────────────┤
│ • SOLO maneja UI y navegación                                   │
│ • Usa ViewBinding para acceder a vistas                         │
│ • Observa LiveData del ViewModel                                │
│ • Delega toda la lógica al ViewModel                           │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│ LIVEDATA (androidx.lifecycle)                                   │
├─────────────────────────────────────────────────────────────────┤
│ • Patrón Observer lifecycle-aware                               │
│ • Notifica cambios automáticamente                             │
│ • Previene memory leaks                                         │
│ • Respeta el ciclo de vida de Activities/Fragments              │
└─────────────────────────────────────────────────────────────────┘


═══════════════════════════════════════════════════════════════════
                    VENTAJAS DE ESTA ARQUITECTURA
═══════════════════════════════════════════════════════════════════

✅ SEPARACIÓN DE RESPONSABILIDADES
   Cada capa tiene un propósito específico

✅ TESTABILIDAD
   ViewModels y Repositories son fáciles de testear

✅ MANTENIBILIDAD
   Cambios en una capa no afectan las demás

✅ ESCALABILIDAD
   Fácil agregar nuevas funcionalidades

✅ REUTILIZACIÓN
   Repositories y ViewModels pueden compartirse

✅ LIFECYCLE AWARE
   No hay memory leaks ni crashes por ciclo de vida

✅ SINGLE SOURCE OF TRUTH
   Los datos vienen de un solo lugar (Repository)


═══════════════════════════════════════════════════════════════════
