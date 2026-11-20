//
// Created by aeliux on 11/19/26.
//

#pragma once

#include "JniCallable.hpp"
#include "shared.hpp"
#include <thread>
#include <cstdint>
#include <memory>

namespace ptrw {

    enum class PointerWrapperFlags : std::uint8_t {
        None           = 0,
        ThreadConfined = 1u << 0,
    };

    constexpr PointerWrapperFlags operator|(PointerWrapperFlags a, PointerWrapperFlags b) noexcept {
        return static_cast<PointerWrapperFlags>(
                static_cast<std::uint8_t>(a) | static_cast<std::uint8_t>(b));
    }
    constexpr PointerWrapperFlags& operator|=(PointerWrapperFlags& a, PointerWrapperFlags b) noexcept {
        a = a | b;
        return a;
    }
    constexpr PointerWrapperFlags operator&(PointerWrapperFlags a, PointerWrapperFlags b) noexcept {
        return static_cast<PointerWrapperFlags>(
                static_cast<std::uint8_t>(a) & static_cast<std::uint8_t>(b));
    }
    constexpr bool hasFlag(PointerWrapperFlags flags, PointerWrapperFlags test) noexcept {
        return (static_cast<std::uint8_t>(flags) & static_cast<std::uint8_t>(test)) != 0;
    }

    class PTRW_API PointerWrapper
    {
    public:
        using Deleter = void (*)(void*) noexcept;
        virtual ~PointerWrapper() noexcept;

        // Ownership must not be accidentally duplicated.
        PointerWrapper(const PointerWrapper&) = delete;
        PointerWrapper& operator=(const PointerWrapper&) = delete;
        PointerWrapper(PointerWrapper&&) = delete;
        PointerWrapper& operator=(PointerWrapper&&) = delete;

        [[nodiscard]] void *getRawPointer() const;
        [[nodiscard]] PointerWrapperFlags getFlags() const noexcept;

        [[nodiscard]] bool isThreadConfined() const noexcept;
        [[nodiscard]] bool isOwnerThread() const noexcept;
        [[nodiscard]] bool isClosed() const noexcept;
        void close();
    protected:
        PointerWrapper(void* pointer, Deleter native_deleter, std::shared_ptr<JniCallable> jni_deleter, PointerWrapperFlags flags = PointerWrapperFlags::ThreadConfined);
        void assertAllowedThread() const;
        void assertOpen() const;
    private:
        struct Impl;
        std::unique_ptr<Impl> m_impl;
    };

} // ptrw
