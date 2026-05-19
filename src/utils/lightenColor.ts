export function lightenColor(hex: string, amount: number): string {
    // Elimina el símbolo '#' si está presente
    hex = hex.replace('#', '');

    // Divide el color en componentes rojo, verde y azul
    let r = parseInt(hex.substring(0, 2), 16);
    let g = parseInt(hex.substring(2, 4), 16);
    let b = parseInt(hex.substring(4, 6), 16);

    // Calcula una versión más clara del color mezclándolo con el gris
    r = Math.min(255, Math.round(r + (255 - r) * amount));
    g = Math.min(255, Math.round(g + (255 - g) * amount));
    b = Math.min(255, Math.round(b + (255 - b) * amount));

    // Devuelve el color en formato hexadecimal
    return `#${r.toString(16).padStart(2, '0')}${g.toString(16).padStart(2, '0')}${b.toString(16).padStart(2, '0')}`;
}