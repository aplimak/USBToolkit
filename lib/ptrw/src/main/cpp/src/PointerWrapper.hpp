//
// Created by aeliux on 11/19/26.
//

#pragma once

#include "shared.hpp"
#include <thread>

namespace ptrw {

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
        [[nodiscard]] bool isOwnerThread() const noexcept;
        [[nodiscard]] bool isClosed() const noexcept;
        void close();
    protected:
        PointerWrapper(void* pointer, Deleter deleter);
        void assertOwnerThread() const;
        void assertOpen() const;
    private:
        void release() noexcept;

        struct Impl;
        Impl* m_impl;
    };

} // ptrw
