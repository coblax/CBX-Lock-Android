cmake_minimum_required(VERSION 3.22.1)

project(examlock_native)

add_library(
    examlock_native
    SHARED
    examlock_native.cpp
    reverse_guard.cpp
    integrity_guard.cpp
    runtime_string_codec.cpp
    qr_crypto.cpp
    clipboard_guard.cpp
    admin_secret.cpp
    bypass_crypto.cpp
    geofence_geometry.cpp
    sha256.cpp
    third_party/miniz/miniz_reader.cpp
)

target_include_directories(
    examlock_native
    PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_CURRENT_SOURCE_DIR}/third_party/miniz
)

target_compile_features(examlock_native PRIVATE cxx_std_17)

target_link_options(examlock_native PRIVATE
    "-Wl,-z,max-page-size=16384"
    "-Wl,-z,common-page-size=16384"
)

find_library(log-lib log)
find_library(z-lib z)

target_link_libraries(
    examlock_native
    PRIVATE
    ${log-lib}
    ${z-lib}
)
