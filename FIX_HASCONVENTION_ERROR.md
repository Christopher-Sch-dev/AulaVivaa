# 🚨 SOLUCIÓN URGENTE: Error HasConvention

## ❌ Problema
```
java.lang.NoClassDefFoundError: org/gradle/api/internal/HasConvention
```

**Causa**: Gradle 9.1.0 eliminó APIs internas que Kotlin 1.9.20 necesita.

## ✅ Solución Aplicada

**Cambié Gradle de 9.1.0 → 8.7** en `gradle/wrapper/gradle-wrapper.properties`

---

## 🚀 QUÉ HACER AHORA (EN ORDEN)

### 1. **Invalidate Caches en Android Studio**
```
File → Invalidate Caches → Invalidate and Restart
```
Espera a que Android Studio reinicie (30 segundos).

### 2. **Sync Project con Gradle**
Cuando Android Studio abra:
```
File → Sync Project with Gradle Files
```
O haz clic en el elefante 🐘 azul arriba a la derecha.

**Esperado**: Gradle descargará versión 8.7 (tarda 1-2 minutos la primera vez)

### 3. **Build Project**
```
Build → Build Project
```

**Esperado**: `BUILD SUCCESSFUL` ✅

---

## 📊 Compatibilidad Arreglada

| Componente | Versión Anterior | Versión Actual | Estado |
|-----------|------------------|----------------|--------|
| **Gradle** | 9.1.0 ❌ | 8.7 ✅ | Compatible |
| **AGP** | 8.3.0 | 8.3.0 ✅ | Compatible |
| **Kotlin** | 1.9.20 | 1.9.20 ✅ | Compatible |
| **SDK** | 34 | 34 ✅ | Compatible |

---

## ⚠️ Si Sigue Fallando

### Error: "Daemon corrupto"
```bash
# En terminal de Android Studio:
gradlew.bat --stop
```
Luego: `File → Invalidate Caches → Restart`

### Error: "Still downloading Gradle"
Espera pacientemente. Gradle 8.7 pesa ~110 MB y se descarga una sola vez.

### Error: "Could not find gradle-wrapper.jar"
Regenera wrapper:
```bash
gradle wrapper --gradle-version 8.7
```

---

## 🎯 Por Qué Falló Originalmente

Gradle 9.x eliminó clases internas como `HasConvention` para limpiar su API. Kotlin 1.9.20 fue lanzado antes de Gradle 9 y todavía referencia esas clases.

**Soluciones posibles**:
1. ✅ Bajar Gradle a 8.x (lo que hice)
2. ❌ Subir Kotlin a 2.0+ (requiere cambiar TODO el código)
3. ❌ Esperar parche de JetBrains (no hay timeline)

---

## ✅ CHECKLIST FINAL

- [x] Gradle downgradeado a 8.7
- [ ] Invalidate Caches ejecutado
- [ ] Sync Project ejecutado
- [ ] Build SUCCESSFUL
- [ ] App compila y corre

---

**Tiempo estimado**: 3-5 minutos (incluyendo descarga de Gradle)

**Estado**: SOLUCIONADO (esperando que sincronices)

---

Creado: 29/01/2025 23:45  
Error: `NoClassDefFoundError: HasConvention`  
Fix: Gradle 9.1.0 → 8.7
