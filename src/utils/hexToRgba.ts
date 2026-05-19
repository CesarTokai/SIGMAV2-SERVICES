export function hexToRgba(hex: string, opacity: number): string {
    // Elimina el símbolo '#' si está presente
    hex = hex.replace('#', '');

    // Divide el color en componentes rojo, verde y azul
    let r = parseInt(hex.substring(0, 2), 16);
    let g = parseInt(hex.substring(2, 4), 16);
    let b = parseInt(hex.substring(4, 6), 16);

    // Devuelve el color en formato rgba
    return `rgba(${r}, ${g}, ${b}, ${opacity})`;
}