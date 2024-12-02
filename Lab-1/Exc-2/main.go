package main

import (
	"fmt"
	"math"
	"strings"
	"time"
	"zad2/ansi"
	"zad2/thr"

	tsize "github.com/kopoli/go-terminal-size"
	"github.com/wzshiming/getch"
)

type task struct {
	gr     thr.GoRoutine
	status status
}

type status string

const (
	running = status("running")
	stopped = status("stopped")
	offline = status("offline")
)

func InitConsole(h uint, tasks []task, selected int) {
	ts, err := tsize.GetSize()
	if err != nil {
		ts = tsize.Size{Height: 10}
	}
	fmt.Print(ansi.Cur.Scroll.Down(uint(ts.Height)))
	fmt.Print(ansi.Cur.GoTo.XY(math.MaxUint16, 1))
	fmt.Print(ansi.Cur.Up(h))

	PrintfWithStatus(
		SprintTasks(tasks, selected),
		taskH,
		""+
			" %v←↓↑→%v move selection\n"+
			"%vspace%v toggle task state\n"+
			"    %vq%v quit\n"+
			"%v\n",
		ansi.Fg.Code(166),
		ansi.Fg.Code(244),
		ansi.Fg.Code(166),
		ansi.Fg.Code(244),
		ansi.Fg.Code(166),
		ansi.Fg.Code(244),
		ansi.Fmt.Reset,
	)
}

const taskH = 3

func SprintTasks(tasks []task, selected int) string {
	s := ansi.Clr.Line.All +
		ansi.Fg.Code(66) +
		strings.Repeat("-", 12*5-2) + "\n"
	for i, v := range tasks {
		if i%5 == 0 && i != 0 {
			s += "\n" + ansi.Clr.Line.All
		}
		if selected == i {
			switch tasks[i].status {
			case running:
				s += ansi.Fg.Code(193)
			case stopped:
				s += ansi.Fg.Code(159)
			case offline:
				s += ansi.Fg.Code(203)
			}
			s += fmt.Sprintf("%v[%v]  ", (i+1)%len(tasks), v.status)
		} else {
			switch tasks[i].status {
			case running:
				s += ansi.Fg.Code(64)
			case stopped:
				s += ansi.Fg.Code(66)
			case offline:
				s += ansi.Fg.Code(131)
			}
			s += fmt.Sprintf("%v[%v]  ", (i+1)%len(tasks), v.status)
		}
		s += ansi.Fmt.Reset
	}

	return s

}

func PrintfWithStatus(
	statusBar string,
	barHeight uint,
	format string,
	a ...any,
) {
	str := ansi.Clr.Screen.ToEnd
	s := strings.Split(fmt.Sprintf(format, a...), "\n")
	for _, v := range s[:len(s)-1] {
		str += fmt.Sprintln(v)
		str += ansi.Cur.Scroll.Up(1)
		str += ansi.Cur.Up(1)
	}
	str += s[len(s)-1]

	str += ansi.Cur.Memo.Save
	str += "\n"
	str += statusBar
	str += ansi.Cur.Memo.Restore

	fmt.Print(str)
}

func Getch() rune {
	k, _, err := getch.Getch()
	if err != nil {
		panic("Something is terribly wrong witch terminal")
	}
	return k
}

// :== main ==: //

func main() {

	tasks := make([]task, 10)
	sel := 0

	for i := range tasks {
		j := 0
		tasks[i].gr = thr.NewGoRoutine(
			func() bool {
				PrintfWithStatus(
					SprintTasks(tasks, sel),
					3,
					"%c%d\n",
					j+'A',
					(i+1)%10,
				)
				j = (j + 1) % ('Z' - 'A' + 1)
				time.Sleep(1 * time.Second)
				return false
			},
		)
		go tasks[i].gr.Start(true)
		tasks[i].status = stopped
	}

	InitConsole(taskH, tasks, sel)

	updateFooter := func() { PrintfWithStatus(SprintTasks(tasks, sel), 3, "") }

	defer func() {
		for i := range tasks {
			tasks[i].gr.Signal(thr.Stop)
			tasks[i].gr.Join()
			tasks[i].status = offline
			updateFooter()
			time.Sleep(10 * time.Millisecond)
		}
		fmt.Print(ansi.Clr.Screen.ToEnd)
		fmt.Println(ansi.Fg.Code(207))
		fmt.Println("-*. Goodbye .*-" + ansi.Fmt.Reset)
	}()

mainLoop:
	for {
		switch Getch() {
		case getch.Key_q, getch.KeyQ:
			break mainLoop
		case getch.KeyUp, getch.KeyDown:
			sel = (sel + 5) % 10
			updateFooter()
		case getch.KeyLeft:
			prevSel := sel
			sel = (sel + 9) % 10
			if (sel < 5) != (prevSel < 5) {
				sel = (sel + 5) % 10
			}
			updateFooter()
		case getch.KeyRight:
			prevSel := sel
			sel = (sel + 11) % 10
			if (sel < 5) != (prevSel < 5) {
				sel = (sel + 5) % 10
			}
			updateFooter()
		case getch.KeySpace:
			if tasks[sel].status == running {
				tasks[sel].status = stopped
				tasks[sel].gr.Signal(thr.Pause)
			} else {
				tasks[sel].status = running
				tasks[sel].gr.Signal(thr.Resume)
			}
			updateFooter()
		}
	}
}
