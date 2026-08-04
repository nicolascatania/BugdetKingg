export function formatDate(date: Date): string {
    const pad = (n: number) => n.toString().padStart(2, '0');

    return `${pad(date.getDate())}/${pad(date.getMonth() + 1)}/${date.getFullYear()} ` +
        `${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

/**
 * Builds the first/last day string range for a given month, formatted to match
 * native <input type="date"> ('yyyy-MM-dd') or <input type="datetime-local">
 * ('yyyy-MM-ddTHH:mm') value formats, depending on whether the target filter
 * carries a time component.
 */
export function getMonthDateRange(
    monthIndex: number,
    year: number,
    includeTime = false,
): { from: string; to: string } {
    const pad = (n: number) => n.toString().padStart(2, '0');
    const toDateString = (date: Date) =>
        `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;

    const firstDay = new Date(year, monthIndex, 1);
    const lastDay = new Date(year, monthIndex + 1, 0);

    if (!includeTime) {
        return { from: toDateString(firstDay), to: toDateString(lastDay) };
    }

    return {
        from: `${toDateString(firstDay)}T00:00`,
        to: `${toDateString(lastDay)}T23:59`,
    };
}
