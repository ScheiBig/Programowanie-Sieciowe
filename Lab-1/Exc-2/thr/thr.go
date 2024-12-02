package thr

type SignalMsg int

const (
	Resume SignalMsg = 0
	Pause  SignalMsg = 1
	Stop   SignalMsg = 2
)

type GoRoutine struct {
	signal  chan SignalMsg
	join    chan struct{}
	success bool
	runPart func() bool
}

func NewGoRoutine(runPart func() bool) GoRoutine {
	return GoRoutine{
		signal:  make(chan SignalMsg),
		join:    make(chan struct{}),
		runPart: runPart,
	}
}

func (gr *GoRoutine) Signal(sig SignalMsg) {
	gr.signal <- sig
}

func (gr *GoRoutine) Join() {
	<-gr.join
}

func (gr GoRoutine) Success() bool {
	return gr.success
}

func (gr *GoRoutine) Start(waitForStart bool) {
	defer close(gr.join)
	if waitForStart {
	wait:
		for {
			switch <-gr.signal {
			case Resume:
				break wait
			case Stop:
				gr.success = false
				return
			}
		}
	}
	for {
		select {
		case signal := <-gr.signal:
			switch signal {
			case Resume:
				continue
			case Stop:
				gr.success = false
				return
			case Pause:
				for newSignal := range gr.signal {
					if newSignal == Resume {
						break
					} else if newSignal == Stop {
						gr.success = false
						return
					}
				}
			}
		default:
			if gr.runPart() {
				gr.success = true
				return
			}
		}
	}
}
