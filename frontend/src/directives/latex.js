import katex from 'katex'
import 'katex/dist/katex.min.css'

const INLINE_OPTS = { throwOnError: false, displayMode: false }
const BLOCK_OPTS = { throwOnError: false, displayMode: true }

export const latex = {
  mounted(el, binding) {
    renderElement(el, binding.value)
  },
  updated(el, binding) {
    const text = String(binding.value ?? '')
    if (el._latexSource === text) return
    renderElement(el, binding.value)
  }
}

function renderElement(el, value) {
  const text = String(value ?? '')
  el._latexSource = text

  // Render to a detached container first to avoid FOUC and DOM conflicts
  const container = document.createElement('div')
  container.style.display = 'contents'

  if (!text.trim()) {
    // Clear children safely
    while (el.firstChild) el.removeChild(el.firstChild)
    return
  }

  const processed = preprocessLatex(text)
  const parts = tokenizeLatex(processed)
  parts.forEach(part => {
    if (part.type === 'block') {
      const div = document.createElement('div')
      div.className = 'katex-block'
      div.innerHTML = katex.renderToString(part.content, BLOCK_OPTS)
      container.appendChild(div)
    } else if (part.type === 'inline') {
      const span = document.createElement('span')
      span.innerHTML = katex.renderToString(part.content, INLINE_OPTS)
      container.appendChild(span)
    } else {
      container.appendChild(document.createTextNode(part.content))
    }
  })

  // Atomic DOM swap
  while (el.firstChild) el.removeChild(el.firstChild)
  while (container.firstChild) el.appendChild(container.firstChild)
}

/**
 * Preprocess text to handle:
 * 1. Literal \n -> actual newlines
 * 2. Raw LaTeX commands without $...$ delimiters -> auto-wrap
 */
function preprocessLatex(text) {
  text = text.replace(/\\n/g, '\n')

  const cmds = [
    'lim','limits','frac','sqrt','sum','prod','oint','int','text','begin','end',
    'left','right','partial','nabla','overline','underline','widehat',
    'overrightarrow','vec','bar','hat','dot','ddot','cong','approx','equiv',
    'neq','leq','geq','infty','pm','mp','cdot','times','div','ast','circ',
    'bullet','star','odot','oplus','otimes','wedge','vee','neg','forall',
    'exists','cap','cup','subset','supset','subseteq','supseteq','emptyset',
    'varnothing','mathbb','mathcal','mathrm','bold','bf','it','sf','tt',
    'quad','qquad','enspace','thinspace','to','rightarrow','leftarrow',
    'leftrightarrow','Rightarrow','Leftarrow','Leftrightarrow','mapsto',
    'implies','iff','sim','simeq','propto','perp','parallel','angle',
    'triangle','Box','diamond','clubsuit','heartsuit','spadesuit',
    'diamondsuit','sharp','flat','natural','hbar','ell','Re','Im','aleph',
    'wp','mid','nmid','lceil','rceil','lfloor','rfloor','lvert','rvert',
    'lVert','rVert','langle','rangle','lgroup','rgroup','brace','brack',
    'vert','bmod','pmod','gcd','lcm','min','max','sup','inf','arg','deg',
    'det','dim','hom','ker','Pr','sin','cos','tan','cot','sec','csc',
    'log','ln','exp','arg','sinh','cosh','tanh'
  ]

  const alpha = [
    'alpha','beta','gamma','delta','epsilon','varepsilon','zeta','eta',
    'theta','vartheta','iota','kappa','lambda','mu','nu','xi','omicron',
    'pi','varpi','rho','sigma','varsigma','tau','upsilon','phi','varphi',
    'chi','psi','omega','Gamma','Delta','Theta','Lambda','Xi','Pi',
    'Sigma','Upsilon','Phi','Psi','Omega'
  ]

  const allCmds = [...cmds, ...alpha]
  const pattern = new RegExp(
    `\\\\(${allCmds.join('|')})\\b`,
    'g'
  )

  text = text.replace(pattern, (match, cmd) => {
    if (isInsideDollar(text, match)) return match
    return `$\\${cmd}`
  })

  return text
}

/**
 * Check if a matched position is already inside $...$ or $$...$$
 */
function isInsideDollar(text, match) {
  const idx = text.indexOf(match)
  if (idx === -1) return false

  let dollarCount = 0
  for (let i = 0; i < idx; i++) {
    if (text[i] === '$' && (i === 0 || text[i - 1] !== '\\')) {
      dollarCount++
    }
  }
  return dollarCount % 2 === 1
}

function tokenizeLatex(text) {
  const tokens = []
  const len = text.length
  let i = 0
  let plainBuffer = ''

  function pushPlain() {
    if (plainBuffer) {
      tokens.push({ type: 'text', content: plainBuffer })
      plainBuffer = ''
    }
  }

  while (i < len) {
    if (text[i] === '$' && i + 1 < len && text[i + 1] === '$') {
      pushPlain()
      const end = text.indexOf('$$', i + 2)
      if (end !== -1) {
        const math = text.substring(i + 2, end).trim()
        if (math) tokens.push({ type: 'block', content: math })
        i = end + 2
        continue
      }
      plainBuffer += '$'
      i++
      continue
    }

    if (text[i] === '$') {
      let end = -1
      for (let j = i + 1; j < len; j++) {
        if (text[j] === '$' && !(j + 1 < len && text[j + 1] === '$')) {
          end = j
          break
        }
      }
      if (end !== -1) {
        const math = text.substring(i + 1, end).trim()
        if (math) {
          pushPlain()
          tokens.push({ type: 'inline', content: math })
        } else {
          plainBuffer += '$'
        }
        i = end + 1
        continue
      }
      plainBuffer += '$'
      i++
      continue
    }

    plainBuffer += text[i]
    i++
  }

  pushPlain()
  return tokens
}
