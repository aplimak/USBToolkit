# AXSN's central third-party registry.
# Register dependencies here, but do not add their source directories eagerly.

axsn_register_vendored_dependency(SHARED NAME googletest
    SOURCE_DIR "${AXSN_ROOT}/third_party/googletest"
    TARGETS GTest::gtest GTest::gtest_main
    EXCLUDE_FROM_ALL
)
