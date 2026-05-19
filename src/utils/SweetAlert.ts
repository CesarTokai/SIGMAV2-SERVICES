import type { SweetAlertIcon, SweetAlertResult } from "sweetalert2";
import Swal from "sweetalert2";


export const VoidAlert = (
    icon: SweetAlertIcon = 'question',
    title: string,
    text: string,
    confirmButtonText: string,
    onConfirm: () => void
): void => {
    Swal.fire({
        icon: icon,
        title: title,
        text: text,
        showCancelButton: true,
        cancelButtonText: "Cancelar",
        confirmButtonColor: 'var(--primary-color)',
        customClass: {
            confirmButton: 'custom-confirm-button',
        },
        confirmButtonText: confirmButtonText,
    }).then((result: SweetAlertResult) => {
        if (result.isConfirmed) {
            onConfirm();
        }
    });
};

export const ToastError = (title: string, text: string): void => {
    Swal.fire({
        icon: 'error',
        title: title,
        text: text,
        showConfirmButton: false,
        timer: 3000,
        toast: true,
        position: "top-end",
    });
};

export const ToastWarning = (title: string, text: string): void => {
    Swal.fire({
        icon: 'warning',
        title: title,
        text: text,
        showConfirmButton: false,
        timer: 3000,
        toast: true,
        position: "top-end",
    });
};

export const ToastSuccess = (title: string, text: string): void => {
    Swal.fire({
        icon: 'success',
        title: title,
        text: text,
        showConfirmButton: false,
        timer: 3000,
        toast: true,
        position: "top-end",
    });
};

export const LoadAlert = (show: boolean): void => {
    if (show) {
        Swal.fire({
            title: 'Cargando...',
            allowOutsideClick: false,
            didRender: () => {
                Swal.showLoading();
            },
            didOpen: () => {
                Swal.showLoading();
            }
        });
    } else {
        Swal.close();
    }
};

export default {
    VoidAlert,
    ToastError,
    ToastWarning,
    ToastSuccess,
    LoadAlert
};