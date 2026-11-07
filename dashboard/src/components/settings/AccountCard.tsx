import { Avatar, Box, Paper, Typography } from '@mui/material'

import '../../styles/AccountCard.css'

type AccountCardProps = {
  userId: string
}

export default function AccountCard({ userId }: AccountCardProps) {
  return (
    <Paper variant="outlined" className="account-card">
      <Avatar className="account-card__avatar">
        {userId.charAt(0).toUpperCase()}
      </Avatar>

      <Box className="account-card__identity">
        <Typography className="account-card__name">{userId}</Typography>

        <Typography variant="body2" color="text.secondary">
          Dashboard account
        </Typography>
      </Box>

      <span className="account-card__status">Signed in</span>
    </Paper>
  )
}