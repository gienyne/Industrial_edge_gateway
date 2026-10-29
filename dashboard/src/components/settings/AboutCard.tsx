import { Box, Paper, Typography } from '@mui/material'

import { APP_INFO } from '../../utils/appInfo'

import '../../styles/AboutCard.css'

export default function AboutCard() {
  return (
    <Paper variant="outlined" className="about-card">
      <Box className="about-card__intro">
        <Box className="about-card__intro-text">
          <Typography className="about-card__name">{APP_INFO.name}</Typography>

          <Typography className="about-card__description">
            {APP_INFO.description}
          </Typography>
        </Box>

        <span className="about-card__version">v{APP_INFO.version}</span>
      </Box>

      <Box className="about-card__block about-card__block--flow">
        <Typography className="about-card__block-title">
          How the data travels
        </Typography>

        <ol className="about-card__pipeline">
          {APP_INFO.flow.map((step, index) => (
            <li key={step.label} className="about-card__node">
              <span className="about-card__dot">{index + 1}</span>
              <span className="about-card__node-label">{step.label}</span>
              <span className="about-card__node-detail">{step.detail}</span>
            </li>
          ))}
        </ol>
      </Box>

      <Box className="about-card__columns">
        <Box className="about-card__block">
          <Typography className="about-card__block-title">Built with</Typography>

          <Box className="about-card__stack">
            {APP_INFO.stack.map((group) => (
              <Box key={group.layer} className="about-card__stack-row">
                <span className="about-card__stack-layer">{group.layer}</span>

                <Box className="about-card__chips">
                  {group.items.map((item) => (
                    <span key={item} className="about-card__chip">
                      {item}
                    </span>
                  ))}
                </Box>
              </Box>
            ))}
          </Box>
        </Box>

        <Box className="about-card__block">
          <Typography className="about-card__block-title">Credits</Typography>

          <Box className="about-card__credits">
            {APP_INFO.credits.map((credit) => (
              <Box key={credit.label} className="about-card__credit">
                <span className="about-card__credit-label">{credit.label}</span>
                <span className="about-card__credit-value">{credit.value}</span>
              </Box>
            ))}
          </Box>
        </Box>
      </Box>
    </Paper>
  )
}