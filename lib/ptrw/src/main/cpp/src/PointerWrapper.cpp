//
// Created by aeliux on 11/19/26.
//

#include "PointerWrapper.hpp"
#include <stdexcept>

namespace ptrw {

    PointerWrapper::PointerWrapper(
            void* pointer,
            Deleter deleter
    )
            : m_pointer(nullptr),
              m_deleter(deleter),
              m_owner(std::this_thread::get_id()) {

        if (pointer == nullptr) {
            throw std::invalid_argument(
                    "PointerWrapper cannot wrap a null pointer"
            );
        }
        if (deleter == nullptr) {
            throw std::invalid_argument("Deleter cannot be null");
        }

        m_pointer = pointer;
    }

    PointerWrapper::~PointerWrapper() noexcept {
        if (isClosed()) return;
        if (!isOwnerThread()) {
            std::terminate();
        }
        release();
    }

    void PointerWrapper::assertOwnerThread() const
    {
        if (!isOwnerThread()) {
            throw std::runtime_error(
                    "PointerWrapper accessed from non-owner thread"
            );
        }
    }

    void PointerWrapper::assertOpen() const
    {
        if (isClosed()) {
            throw std::runtime_error(
                    "PointerWrapper is closed"
            );
        }
    }

    void PointerWrapper::close() {
        assertOwnerThread();
        release();
    }

    void PointerWrapper::release() noexcept {
        if (isClosed())
            return;

        void* pointer = m_pointer;
        m_pointer = nullptr;

        m_deleter(pointer);
    }

    void *PointerWrapper::getRawPointer() const
    {
        assertOpen();
        assertOwnerThread();
        return m_pointer;
    }

    bool PointerWrapper::isOwnerThread() const noexcept
    {
        return m_owner == std::this_thread::get_id();
    }

    bool PointerWrapper::isClosed() const noexcept
    {
        return m_pointer == nullptr;
    }
} // ptrw
