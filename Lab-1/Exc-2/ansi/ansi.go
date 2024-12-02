package ansi

import (
	"fmt"
)

type makeEscSeq func(uint) string
type make2EscSeq func(uint, uint) string
type makeEscSeq8 func(uint8) string

type colorMap struct {
	Black   string
	Red     string
	Green   string
	Yellow  string
	Blue    string
	Magenta string
	Cyan    string
	White   string
}

type colorPalete struct {
	colorMap
	Bright colorMap
	Code   makeEscSeq8
}

type formatMap struct {
	Bold      string
	Faint     string
	Italic    string
	Underline string
	Inverse   string
}

type formatSpecifiers struct {
	Reset string
	formatMap
	Off formatMap
}

type clearingMap struct {
	ToEnd   string
	ToStart string
	All     string
}

type clearingSpecifiers struct {
	Screen     clearingMap
	Line       clearingMap
	Everything string
}

type cursorMemory struct {
	Save    string
	Restore string
}

type cursorMove struct {
	Up   makeEscSeq
	Down makeEscSeq
}

type cursorGo struct {
	XY make2EscSeq
	X  makeEscSeq
}

type cursorSpecifiers struct {
	Up     makeEscSeq
	Down   makeEscSeq
	Right  makeEscSeq
	Left   makeEscSeq
	Line   cursorMove
	Scroll cursorMove
	GoTo   cursorGo
	Memo   cursorMemory
}

var Fg = colorPalete{
	colorMap: colorMap{
		Black:   "\u001b[30m",
		Red:     "\u001b[31m",
		Green:   "\u001b[32m",
		Yellow:  "\u001b[33m",
		Blue:    "\u001b[34m",
		Magenta: "\u001b[35m",
		Cyan:    "\u001b[36m",
		White:   "\u001b[37m",
	},
	Bright: colorMap{
		Black:   "\u001b[90m",
		Red:     "\u001b[91m",
		Green:   "\u001b[92m",
		Yellow:  "\u001b[93m",
		Blue:    "\u001b[94m",
		Magenta: "\u001b[95m",
		Cyan:    "\u001b[96m",
		White:   "\u001b[97m",
	},
	Code: func(c uint8) string { return fmt.Sprintf("\u001b[38;5;%vm", c) },
}

var Bg = colorPalete{
	colorMap: colorMap{
		Black:   "\u001b[40m",
		Red:     "\u001b[41m",
		Green:   "\u001b[42m",
		Yellow:  "\u001b[43m",
		Blue:    "\u001b[44m",
		Magenta: "\u001b[45m",
		Cyan:    "\u001b[46m",
		White:   "\u001b[47m",
	},
	Bright: colorMap{
		Black:   "\u001b[100m",
		Red:     "\u001b[101m",
		Green:   "\u001b[102m",
		Yellow:  "\u001b[103m",
		Blue:    "\u001b[104m",
		Magenta: "\u001b[105m",
		Cyan:    "\u001b[106m",
		White:   "\u001b[107m",
	},
	Code: func(c uint8) string { return fmt.Sprintf("\u001b[48;5;%vm", c) },
}

var Fmt = formatSpecifiers{
	Reset: "\u001b[0m",
	formatMap: formatMap{
		Bold:      "\u001b[1m",
		Faint:     "\u001b[2m",
		Italic:    "\u001b[3m",
		Underline: "\u001b[4m",
		Inverse:   "\u001b[7m",
	},
	Off: formatMap{
		Bold:      "\u001b[22m",
		Faint:     "\u001b[22m",
		Italic:    "\u001b[23m",
		Underline: "\u001b[24m",
		Inverse:   "\u001b[27m",
	},
}

var Clr = clearingSpecifiers{
	Screen: clearingMap{
		ToEnd:   "\u001b[0J",
		ToStart: "\u001b[1J",
		All:     "\u001b[2J",
	},
	Line: clearingMap{
		ToEnd:   "\u001b[0K",
		ToStart: "\u001b[1K",
		All:     "\u001b[2K",
	},
	Everything: "\u001b[3J",
}

var Cur = cursorSpecifiers{
	Up:    func(n uint) string { return fmt.Sprintf("\u001b[%vA", n) },
	Down:  func(n uint) string { return fmt.Sprintf("\u001b[%vB", n) },
	Right: func(n uint) string { return fmt.Sprintf("\u001b[%vC", n) },
	Left:  func(n uint) string { return fmt.Sprintf("\u001b[%vD", n) },
	Line: cursorMove{
		Up:   func(n uint) string { return fmt.Sprintf("\u001b[%vF", n) },
		Down: func(n uint) string { return fmt.Sprintf("\u001b[%vE", n) },
	},
	Scroll: cursorMove{
		Up:   func(n uint) string { return fmt.Sprintf("\u001b[%vS", n) },
		Down: func(n uint) string { return fmt.Sprintf("\u001b[%vT", n) },
	},
	GoTo: cursorGo{
		XY: func(y, x uint) string { return fmt.Sprintf("\u001b[%v;%vH", y, x) },
		X:  func(x uint) string { return fmt.Sprintf("\u001b[%vG", x) },
	},
	Memo: cursorMemory{
		Save:    "\u001b[s",
		Restore: "\u001b[u",
	},
}
