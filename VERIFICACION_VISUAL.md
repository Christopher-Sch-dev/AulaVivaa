# 🔍 VERIFICACIÓN VISUAL - AULA VIVA

## Para confirmar que todo está bien en tu Android Studio

---

## 1️⃣ VERIFICAR GRADLE (Build → Build Project)

### Paso 1: Sync Gradle
- Abre `File` → `Sync Now`
- Espera mensaje: `Gradle sync finished`
- Si ves ❌ en las líneas rojas, no es grave, solo warnings

### Paso 2: Revisar build.gradle.kts
Abre `app/build.gradle.kts` y verifica:

```kotlin
✅ compileSdk = 34          (línea ~12)
✅ targetSdk = 34           (línea ~16)
✅ id("com.google.devtools.ksp") version "1.9.20-1.0.13"  (línea ~7)
✅ // implementation("com.google.ai.client.generativeai...  (comentada, línea ~74)
```

Si ves:
- ❌ compileSdk = 36 → está mal
- ❌ targetSdk = 36 → está mal
- ❌ implementation("com.google.ai.client.generativeai:generativeai:0.1.2") sin comentar → mal

### Paso 3: Revisar libs.versions.toml
Abre `gradle/libs.versions.toml` y verifica:

```toml
✅ agp = "8.3.0"           (no 8.13.0)
✅ kotlin = "1.9.20"       (no 2.2.20)
✅ firebaseBom = "32.7.1"
✅ material = "1.11.0"
```

### Paso 4: Build del proyecto
```bash
Build → Build Project
```

Esperado:
```
Build completed successfully ✅
```

Si falla:
- Copia el error
- Abre Terminal en Android Studio
- Ejecuta: ./gradlew.bat clean build
- Pega aquí el error exacto

---

## 2️⃣ VERIFICAR ESTRUCTURA DE CARPETAS

En el panel izquierdo (Project View), verifica:

```
AulaViva
└── app
    └── src
        └── main
            ├── AndroidManifest.xml ✅
            │   └── Debe tener: <uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
            │   └── Debe tener: 4 activities (Login, Register, PanelPrincipal, ListaClases)
            │
            ├── java/cl/duocuc/aulaviva/
            │   ├── data/
            │   │   ├── local/ ✅
            │   │   │   ├── AppDatabase.kt
            │   │   │   ├── ClaseDao.kt
            │   │   │   └── ClaseEntity.kt
            │   │   ├── model/ ✅
            │   │   │   ├── Usuario.kt
            │   │   │   └── Clase.kt
            │   │   └── repository/ ✅
            │   │       ├── AuthRepository.kt
            │   │       ├── ClaseRepository.kt
            │   │       └── IARepository.kt
            │   │
            │   ├── presentation/
            │   │   ├── ui/ ✅
            │   │   │   ├── auth/
            │   │   │   │   ├── LoginActivity.kt
            │   │   │   │   └── RegisterActivity.kt
            │   │   │   ├── main/
            │   │   │   │   └── PanelPrincipalActivity.kt
            │   │   │   └── clases/
            │   │   │       └── ListaClasesActivity.kt
            │   │   ├── viewmodel/ ✅
            │   │   │   ├── AuthViewModel.kt
            │   │   │   └── ClaseViewModel.kt
            │   │   └── adapter/ ✅
            │   │       └── ClaseAdapter.kt
            │   │
            │   └── utils/
            │       └── NotificationHelper.kt ✅
            │
            ├── res/
            │   ├── layout/ ✅
            │   │   ├── activity_login.xml
            │   │   ├── activity_register.xml
            │   │   ├── activity_panel_principal.xml
            │   │   ├── activity_lista_clases.xml
            │   │   ├── dialog_crear_clase.xml
            │   │   └── item_clase.xml
            │   ├── anim/ ✅
            │   │   ├── button_scale.xml
            │   │   ├── slide_in_right.xml
            │   │   └── slide_out_left.xml
            │   ├── drawable/ ✅
            │   ├── values/ ✅
            │   │   ├── colors.xml
            │   │   ├── themes.xml
            │   │   ├── strings.xml
            │   │   └── dimens.xml
            │   └── mipmap/ ✅
            │
            └── google-services.json ✅
                (Debe estar presente para Firebase)
```

Si falta algo:
- ❌ Alguna carpeta/clase → crea manual
- ❌ google-services.json → descárgalo de Firebase Console

---

## 3️⃣ VERIFICAR CÓDIGO SIN ERRORES

En Android Studio, busca errores con:
`Analyze → Run Inspection by Name → "Android Linter"`

O en Panel izquierdo → Problems → debería estar vacío ✅

Si hay errores:
- Rojo intenso 🔴 = ERROR (crítico, no compila)
- Naranja 🟠 = WARNING (compila pero aviso)
- Azul 🔵 = Info (solo información)

Para resolver:
1. Haz clic en el error
2. Click derecho → "Show Context Actions"
3. Aplica las sugerencias

---

## 4️⃣ VERIFICAR IMPORTS Y REFERENCIAS

En `LoginActivity.kt`, verifica que SÍ ves (sin línea roja):

```kotlin
import android.content.Intent ✅
import androidx.activity.viewModels ✅
import androidx.lifecycle.Observer ✅
import cl.duocuc.aulaviva.databinding.ActivityLoginBinding ✅
import cl.duocuc.aulaviva.presentation.viewmodel.AuthViewModel ✅
import cl.duocuc.aulaviva.presentation.ui.main.PanelPrincipalActivity ✅
```

Si ves línea roja 🔴 en algún import:
- Click derecho en la línea
- "Show intention actions"
- Click en "Import class" o similar

---

## 5️⃣ VERIFICAR MATERIAL 3

Abre `activity_login.xml` y verifica:

```xml
✅ <com.google.android.material.textfield.TextInputLayout>  (Material 3)
✅ <com.google.android.material.textfield.TextInputEditText>
✅ <com.google.android.material.button.MaterialButton>
✅ Usa estilos como: style="@style/Widget.MaterialComponents.TextInputLayout.OutlinedBox"
```

Si ves `android.widget.Button` directamente:
- ❌ No es Material 3
- Cámbialo a `<com.google.android.material.button.MaterialButton>`

Abre `item_clase.xml` y verifica:

```xml
✅ <com.google.android.material.card.MaterialCardView>  (Material 3 card)
✅ Tiene propiedades como: app:cardElevation, app:cardCornerRadius
```

---

## 6️⃣ VERIFICAR ANIMACIONES

Abre carpeta `res/anim/` y verifica que existan:

- `button_scale.xml` ✅ (escala 80% a 100% al presionar)
- `slide_in_right.xml` ✅ (entra desde derecha)
- `slide_out_left.xml` ✅ (sale hacia izquierda)

Abre uno, debe tener:
```xml
<?xml version="1.0" encoding="utf-8"?>
<set xmlns:android="http://schemas.android.com/apk/res/android">
    <scale .../>  o <translate .../>
</set>
```

---

## 7️⃣ VERIFICAR FIREBASE

En `AndroidManifest.xml`:
```xml
✅ <meta-data android:name="com.google.firebase.messaging.default_notification_channel_id" .../>
✅ <service android:name="com.google.firebase.messaging.FirebaseMessagingService" .../>
```

En `app/build.gradle.kts`:
```kotlin
✅ id("com.google.gms.google-services")
✅ implementation(platform(libs.firebase.bom))
✅ implementation(libs.firebase.auth.ktx)
✅ implementation(libs.firebase.firestore.ktx)
✅ implementation(libs.firebase.analytics)
```

En `app/google-services.json`:
- Si falta, descárgalo de https://console.firebase.google.com
- Ubícalo en `app/` (al mismo nivel que build.gradle.kts)

---

## 8️⃣ VERIFICAR ROOM DATABASE

En `data/local/AppDatabase.kt`:
```kotlin
✅ @Database(entities = [ClaseEntity::class], version = 1, exportSchema = false)
✅ abstract class AppDatabase : RoomDatabase()
✅ abstract fun claseDao(): ClaseDao
✅ companion object { fun getDatabase(context: Context): AppDatabase { ... } }
```

En `data/local/ClaseEntity.kt`:
```kotlin
✅ @Entity(tableName = "clases")
✅ @PrimaryKey(autoGenerate = true) val id: Int = 0
✅ @ColumnInfo(name = "nombre") val nombre: String = ""
```

En `data/local/ClaseDao.kt`:
```kotlin
✅ @Dao interface ClaseDao
✅ @Query("SELECT * FROM clases") fun obtenerTodasLasClases(): Flow<List<ClaseEntity>>
✅ @Insert suspend fun insertar(clase: ClaseEntity)
✅ @Delete suspend fun eliminar(clase: ClaseEntity)
```

---

## 9️⃣ VERIFICAR VIEWMODEL + LIVEDATA

En `AuthViewModel.kt`:
```kotlin
✅ class AuthViewModel : ViewModel()
✅ private val _isLoading = MutableLiveData<Boolean>()
✅ val isLoading: LiveData<Boolean> = _isLoading
✅ fun login(email: String, password: String) { ... }
✅ fun register(email: String, password: String, rol: String) { ... }
```

En `LoginActivity.kt`:
```kotlin
✅ private val viewModel: AuthViewModel by viewModels()
✅ viewModel.isLoading.observe(this, Observer { ... })
✅ viewModel.error.observe(this, Observer { ... })
✅ viewModel.loginSuccess.observe(this, Observer { ... })
```

---

## 🔟 VERIFICAR NOTIFICACIONES (Recurso Nativo #1)

En `AndroidManifest.xml`:
```xml
✅ <uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
```

En `NotificationHelper.kt`:
```kotlin
✅ class NotificationHelper(context: Context)
✅ fun mostrarNotificacion(titulo: String, contenido: String)
✅ NotificationManager notificationManager = context.getSystemService(...)
✅ notificationManager.notify(id, notification.build())
```

En `PanelPrincipalActivity.kt`:
```kotlin
✅ private val requestPermissionLauncher = registerForActivityResult(...)
✅ if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
   }
```

---

## 1️⃣1️⃣ VERIFICAR COMPARTIR (Recurso Nativo #2)

En `ClaseAdapter.kt`:
```kotlin
✅ holder.btnCompartir.setOnClickListener {
       val intent = Intent(Intent.ACTION_SEND)
       intent.type = "text/plain"
       intent.putExtra(Intent.EXTRA_TEXT, "Clase: ${clase.nombre}...")
       context.startActivity(Intent.createChooser(intent, "Compartir vía"))
   }
```

---

## 1️⃣2️⃣ TESTS FINALES ANTES DE DEFENDER

### Test 1: ¿Compila?
```bash
./gradlew.bat clean build
```
Debe decir: `BUILD SUCCESSFUL` ✅

### Test 2: ¿Abre?
1. Click derecho en `LoginActivity.kt`
2. Click en "Run LoginActivity"
3. Selecciona emulador
4. Debe abrir sin crashes ✅

### Test 3: ¿Funciona login?
1. Abre RegisterActivity (click "Registrarse")
2. Ingresa: `test@test.com` / `123456` / "Alumno"
3. Click "Registrarse"
4. Click "Volver a login"
5. Ingresa credenciales
6. Click "Iniciar sesión"
7. Deberías ver PanelPrincipalActivity ✅

### Test 4: ¿Funciona crear clase?
1. En PanelPrincipal, busca botón "Crear clase" (si eres docente)
2. Ingresa nombre + fecha
3. Click "Crear"
4. Deberías ver la clase en ListaClases ✅

### Test 5: ¿Funciona compartir?
1. En ListaClases, busca botón "Compartir"
2. Click → debe abrir selector de apps
3. Selecciona WhatsApp, Gmail, etc.
4. ✅ Debe abrir la app con el contenido

### Test 6: ¿Funcionan notificaciones?
1. En PanelPrincipal, acepta permiso POST_NOTIFICATIONS
2. Deberías ver notificación "¡Bienvenido!" ✅

---

## 📊 CHECKLIST VISUAL FINAL

| Elemento | Verificado | Estado |
|----------|-----------|--------|
| compileSdk = 34 | ☐ | |
| targetSdk = 34 | ☐ | |
| Kotlin 1.9.20 | ☐ | |
| AGP 8.3.0 | ☐ | |
| KSP 1.9.20-1.0.13 | ☐ | |
| Gemini comentada | ☐ | |
| Build SUCCESSFUL | ☐ | |
| LoginActivity funciona | ☐ | |
| RegisterActivity funciona | ☐ | |
| PanelPrincipal funciona | ☐ | |
| ListaClases funciona | ☐ | |
| Material 3 visible | ☐ | |
| Animaciones funcionan | ☐ | |
| Compartir funciona | ☐ | |
| Notificaciones funcionan | ☐ | |
| Room guarda datos | ☐ | |
| Firestore sincroniza | ☐ | |

Si todos tienen ✅, ¡estás listo para la defensa!

---

**Creado**: 29/01/2025  
**Tipo**: Verificación Visual en Android Studio  
**Tiempo**: ~15 minutos para completar todos los checks
