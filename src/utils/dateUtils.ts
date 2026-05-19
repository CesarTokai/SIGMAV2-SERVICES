/**
 * Formats a Date object into a string based on the specified format.
 *
 * @param date - The Date object to format
 * @param format - The format for the date string, default is "DD/MM/YYYY"
 * @return The formatted date string
 */
export function formatDate(date: Date, format: string = "DD/MM/YYYY"): string {
    const day = date.getDate().toString().padStart(2, '0');
    const month = (date.getMonth() + 1).toString().padStart(2, '0');
    const year = date.getFullYear();

    switch (format) {
        case "DD/MM/YYYY": return `${day}/${month}/${year}`;
        case "YYYY-MM-DD": return `${year}-${month}-${day}`;
        default: return date.toDateString();
    }
}